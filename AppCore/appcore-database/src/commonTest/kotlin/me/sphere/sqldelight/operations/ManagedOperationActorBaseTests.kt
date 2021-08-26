package me.sphere.sqldelight.operations

import app.cash.turbine.test
import com.squareup.sqldelight.internal.Atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import me.sphere.appcore.utils.freeze
import me.sphere.logging.Logger
import me.sphere.models.AgentId
import me.sphere.models.BackendEnvironmentType
import me.sphere.models.operations.OperationStatus
import me.sphere.sqldelight.*
import me.sphere.test.DbTests
import me.sphere.test.support.TestLogger
import kotlin.test.*
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class ManagedOperationActorBaseTests: DbTests() {
    private fun Scope.makeAndStartActor() = StubActor(database, TestLogger, storeScope)
        .also { it.attach() }

    @Test
    fun should_complete_when_output_is_emitted() = runTesting {
        val actor = makeAndStartActor()
        val testChannel = Channel<String>(BUFFERED)
        actor.action.set { _ -> testChannel.consumeAsFlow() }

        enqueueOperation()

        listenOperation().test {
            assertEquals(OperationStatus.Idle, actual = expectItem().status)
            assertEquals(OperationStatus.Started, actual = expectItem().status)
            assertEquals(false, testChannel.isClosedForReceive, "Channel should not be closed.")

            testChannel.offer("first_value")
            val item = expectItem()
            assertEquals(OperationStatus.Success, actual = item.status)
            assertEquals("\"first_value\"", actual = item.output)

            assertEquals(true, testChannel.isClosedForReceive, "Channel should have been closed.")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun should_fail_when_exception_is_thrown() = runTesting {
        val actor = makeAndStartActor()
        actor.action.set { flow { throw object: RuntimeException() {} } }

        enqueueOperation()

        listenOperation().test {
            assertEquals(OperationStatus.Idle, actual = expectItem().status)
            assertEquals(OperationStatus.Started, actual = expectItem().status)
            assertEquals(OperationStatus.Failure, actual = expectItem().status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun should_fail_when_neither_output_nor_exception_is_emitted() = runTesting {
        val actor = makeAndStartActor()
        actor.action.set { _ -> emptyFlow() }

        enqueueOperation()
        listenOperation().test {
            assertEquals(OperationStatus.Idle, actual = expectItem().status)
            assertEquals(OperationStatus.Started, actual = expectItem().status)
            assertEquals(OperationStatus.Failure, actual = expectItem().status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun should_suspend_when_NeedsSuspensionException_is_thrown_immediately() = runTesting {
        val actor = makeAndStartActor()
        actor.action.set { throw NeedsSuspensionException() }
        enqueueOperation()
        listenOperation().test {
            assertEquals(OperationStatus.Idle, actual = expectItem().status)
            assertEquals(OperationStatus.Started, actual = expectItem().status)
            assertEquals(OperationStatus.Suspended, actual = expectItem().status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun should_suspend_when_NeedsSuspensionException_is_thrown_asynchronously() = runTesting {
        val actor = makeAndStartActor()
        actor.action.set { flow { throw NeedsSuspensionException() } }

        enqueueOperation()
        listenOperation().test {
            assertEquals(OperationStatus.Idle, actual = expectItem().status)
            assertEquals(OperationStatus.Started, actual = expectItem().status)
            assertEquals(OperationStatus.Suspended, actual = expectItem().status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun actor_should_pick_up_reenqueues_of_previously_completed_operation_in_a_busy_concurrent_environment() = runTesting {
        val threadDispatcher = Dispatchers.Default
        val testScope = StoreScope(
            AgentId("stub"),
            StoreClientType.App,
            BackendEnvironmentType.TESTING,
            database.managedOperationQueries,
            TestLogger,
            overrideDispatcher = threadDispatcher
        )
        val actor = StubActor(database, TestLogger, testScope)
            .also { it.attach() }

        /**
         * ## What "re-enqueuing" means?
         *
         * An operation definition may opt into uniquing by key. For uniquing operations, three scenarios may happen
         * when you enqueue via [OperationUtils]:
         *
         * 1. an operation with the same key already exists
         *    i) the operation is idle (awaiting actor) or started
         *       => Use the existing operation — observe the status & output of this instance.
         *          (i.e., effectively, it starts once, and multicasts the results to all subscribers attached when it
         *                 is running. It is semantically equivalent to `replay().autoconnect()` in Combine or Rx).
         *
         *    ii) the operation has completed (success/failure) or is suspended
         *       => Reuse the existing operation — clear the output, overwrite the input, and move the operation back to
         *          idle (awaiting actor).
         *
         * 2. no operation with the key exists
         *    => Create a new instance.
         *
         * ## What is this test case for?
         *
         * This test case is to emulate a busy concurrent environment, leading to database listening [Flow]
         * not re-runing queries as frequent as we expected. The knock-on effect is that the
         * [OperationStoreActorBase] internal might miss status updates of some operations, causing
         * some said re-enqueues intermittently not being picked up by the actor at all.
         *
         * To illustrate the concurrency issue, this is the expected event timeline for re-enqueuing once:
         *
         * 1. << User @ thread 0 >> Enqueue operation input with key = "apple"; Create operation 1 (key = "apple")
         *   Operation status: [OperationStatus.Idle]
         *   Actor to-do list: []
         *
         * 2. << Actor @ thread Z >> Receive data changed notification, and reload the idle operations list from DB.
         *   Operation status: [OperationStatus.Idle]
         *   Actor to-do list: [] -> [1]
         *
         * 3. << Actor @ thread Z >> Detected a new enqueue, so we pick it up.
         *   Operation status: [OperationStatus.Idle] -> [OperationStatus.Started]
         *   Actor to-do list: [1] -> []
         *
         * 4. << Actor @ thread Z >> Completed work for operation 1.
         *   Operation status: [OperationStatus.Started] -> [OperationStatus.Success]
         *   Actor to-do list: []
         *
         * 5. << User @ thread 0 >> Enqueue operation input with key = "apple"; Recycle operation 1 (key = "apple")
         *   Operation status: [OperationStatus.Success] -> [OperationStatus.Idle]
         *   Actor to-do list: []
         *
         * 6. Same as Step 2
         * 7. Same as Step 3
         * 8. Same as Step 4
         *
         * This does seem to work reliably on iOS. However, it intermittently fails on Android. The identified issue was
         * that the data changed notifications generated by Step 3+4 were intermittently executed only after Step 5
         * happens on another thread, due to a variety of factors including:
         *
         * (i) a busy `ActorListen` dispatcher shared by many actors; and
         * (ii) [listenAll] throttles the data changed notifications with a 250ms interval by default.
         *
         * The knock-on effect of delayed notifications is that the internal to-do list of the [OperationStoreActorBase]
         * did not get updated at the expected moments. More specifically, it intermittently did not get to see (metaphorically)
         * that the operation has transitioned to [OperationStatus.Started] or any of the terminal status (success/failure).
         *
         * So when Step 6 kicks in, the [OperationStoreActorBase] compares the reloaded to-do list (`[1]` because of the re-enqueue)
         * against the list it was last notified (also `[1]` and is outdated from our god's view), and erroneously concludes that
         * there is no new enqueue made (by set subtraction, new - old).
         *
         * ## Solution
         * Add information to the operation that can signify changes — e.g. a `lastUpdated` timestamp that is updated
         * every time the operation status has transitioned to another. The actor now then expands its to-do list to
         * track pairs of operation IDs and timestamp.
         *
         * With that, the actor can always see that operation 1 enqueued at Step 1 is different from operation
         * 1 recycled at Step 5, as both would carry a different `lastUpdated` timestamp. This would be the case
         * regardless of the timing of data changed notifications. Therefore, the enqueuing to picking-up process is now
         * concurrency proof.
         */

        // This is a blocking semaphore, not the one from kotlinx-coroutines.
        val actorListenBlocker = me.sphere.test.support.Semaphore(0)

        actor.action.set {
            flowOf("stage 1")
                .onCompletion {
                    /**
                     * Block the `ActorListen` thread until the [semaphore] is signalled below (on the main thread).
                     * This is to emulate a congestion in the shared dispatcher.
                     */
                    testScope.ActorListenScope.launch {
                        actorListenBlocker.waitForPermit()
                    }
                }
        }

        enqueueOperation()
        listenOperation().filter { it.status == OperationStatus.Success }.take(1).test {
            val final = expectItem()
            assertEquals(OperationStatus.Success, actual = final.status)
            assertEquals("\"stage 1\"", actual = final.output)

            cancelAndIgnoreRemainingEvents()
        }

        actor.action.set { flowOf("stage 2") }

        /**
         * Status is transitioned from [OperationStatus.Success] to [OperationStatus.Idle].
         *
         * This emits a queryDataChanged notification on the ActorListen thread, but since the
         * dispatcher is congested, the [OperationStoreActorBase] won't be able to respond to it yet.
         */
        enqueueOperation()

        /**
         * Release the blocked `ActorListen` thread.
         *
         * Now that the thread is unblocked, all the queued queryDataChanged notifications are starting to be processed,
         * with [OperationStoreActorBase] seeing a reloaded list of incomplete operations.
         */
        actorListenBlocker.signalPermit()

        listenOperation().filter { it.status == OperationStatus.Success }.take(1).test {
            val final = expectItem()
            assertEquals(OperationStatus.Success, actual = final.status)
            assertEquals("\"stage 2\"", actual = final.output)

            cancelAndIgnoreRemainingEvents()
        }

        testScope.closeAndJoin()
    }

    private fun enqueueOperation(uniqueKey: String = "unique") = database.managedOperationQueries.enqueue(
        clientType = StoreClientType.App,
        operationType = StubOperation.identifier,
        uniqueKey = uniqueKey,
        input = Json.encodeToString(StubOperation.outputSerializer, "input"),
        lastUpdated = Clock.System.now()
    )

    private fun listenOperation(uniqueKey: String = "unique") = database.managedOperationQueries
        .getOperationById(
            database.managedOperationQueries
                .operationIdForUniqueKey(StoreClientType.App, StubOperation.identifier, uniqueKey)
                .executeAsOne()
        )
        .listenOne(ListenerOption.NoThrottle)
}

private class StubActor(
    database: SqlDatabaseGateway,
    logger: Logger,
    StoreScope: StoreScope
): OperationStoreActorBase<String, String>(database, logger, StoreScope) {
    val action = Atomic<((String) -> Flow<String>)?>(null)
    override val definition = StubOperation

    init { freeze() }

    override suspend fun perform(input: String): String {
        return action.get()?.invoke(input)?.first()
            ?: throw IllegalStateException("Test must supply a `(String) -> Flow<String>`. Found none.")
    }
}
