package me.sphere.sqldelight.operations

import app.cash.turbine.test
import com.squareup.sqldelight.Query
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.datetime.Clock
import me.sphere.models.operations.OperationStatus
import me.sphere.test.DbTests
import me.sphere.sqldelight.ManagedOperation
import me.sphere.sqldelight.StoreClientType
import me.sphere.test.support.TestLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class ManagedOperationTests: DbTests() {
    private fun Scope.makeOperationUtils() = OperationUtils(database, TestLogger, storeScope)
        .also { storeScope.register(makeStubActors()) }

    @Test
    fun should_accept_operations_without_identifier_as_always_distinct() = runTesting {
        val operationUtils = makeOperationUtils()

        operationUtils.enqueue(StubOperation, "content0")
        operationUtils.enqueue(StubOperation, "content1")

        val operations = getAllOperations(StubOperation.identifier)
            .sortedBy(ManagedOperation::input)

        val allInputs = operations.map(ManagedOperation::input)
        val allStatus = operations.map(ManagedOperation::status)

        assertEquals(2, operations.count())
        assertEquals(listOf("\"content0\"", "\"content1\""), actual = allInputs)
        assertEquals(listOf(OperationStatus.Idle, OperationStatus.Idle), actual = allStatus)
    }

    @Test
    fun should_unique_operations_by_identifier_and_keep_older_input() = runTesting {
        val operationUtils = makeOperationUtils()
        val uniqueId = "unique-id-0"

        // Only operations in success or failure state would be restarted with the newer input. Since the operation here
        // is in idle/started state, the assertion must see only the older input.
        val input0 = UniquingStubOperation.Input(id = uniqueId, content = "earlier-content")
        val input1 = UniquingStubOperation.Input(id = uniqueId, content = "newer-content")
        operationUtils.enqueue(UniquingStubOperation, input0)
        operationUtils.enqueue(UniquingStubOperation, input1)

        val operation = getAllOperations(UniquingStubOperation.identifier).single()
        assertEquals(operation.status, OperationStatus.Idle)
        assertEquals(operation.input, """
            {"id":"${uniqueId}","content":"earlier-content"}
        """.trimIndent())
        assertEquals(operation.uniqueKey, uniqueId)
    }

    @Test
    fun should_unique_operations_by_identifier_and_restart_operation_with_newer_input() = runTesting {
        val operationUtils = makeOperationUtils()
        val testUniqueId = "unique-id-1"

        // Only operations in success or failure state would be restarted with the newer input. Since we simulate the
        // first enqueue here being failed, the later assertion must see only the newer input.
        val input0 = UniquingStubOperation.Input(id = testUniqueId, content = "earlier-content")
        operationUtils.enqueue(UniquingStubOperation, input0)

        with(getAllOperations(UniquingStubOperation.identifier).single()) {
            assertEquals(uniqueKey, testUniqueId)
            database.managedOperationQueries.actorSetFailure(Clock.System.now(), null, id)

            val input1 = UniquingStubOperation.Input(id = testUniqueId, content = "newer-content")
            operationUtils.enqueue(UniquingStubOperation, input1)
        }

        with(getAllOperations(UniquingStubOperation.identifier).single()) {
            assertEquals(input, """
            {"id":"${testUniqueId}","content":"newer-content"}
            """.trimIndent())
            assertEquals(uniqueKey, testUniqueId)
            assertEquals(status, OperationStatus.Idle)
        }
    }

    @Test
    fun should_throw_OperationSuspendedException_when_the_operation_is_suspended() = runTesting {
        val operationUtils = makeOperationUtils()

        val id = when (val result = operationUtils.enqueue(StubOperation, "content0")) {
            is EnqueuingResult.Completed -> fail("The stub actor should not have implemented the fast path.")
            is EnqueuingResult.Enqueued -> result.id
        }

        operationUtils.listen(StubOperation, id).test {
            val now = Clock.System.now()
            database.managedOperationQueries.actorSetSuspended(now, id.rawValue)

            val error = expectError()
            assertEquals(OperationSuspendedException::class, actual = error::class)
        }
    }

    private fun getAllOperations(type: String): List<ManagedOperation> = database.managedOperationQueries
        .getOperationsAwaitingActorPickup(StoreClientType.App)
        .executeAsList()
        .filter { it.operationType == type }
        .map { it.id }
        .map(database.managedOperationQueries::getOperationById)
        .map(Query<ManagedOperation>::executeAsOne)

    /**
     * These actors are stubs to fulfill the StoreScope runtime registration requirement. They are not attached with
     * [OperationStoreActorBase.attach] in this unit test.
     */
    private fun Scope.makeStubActors() = listOf(
        object : OperationStoreActorBase<UniquingStubOperation.Input, String>(database, TestLogger, storeScope) {
            override val definition = UniquingStubOperation
            override fun beforeEnqueue(input: UniquingStubOperation.Input): String? = null
            override suspend fun perform(input: UniquingStubOperation.Input): String = suspendForever()
        },
        object : OperationStoreActorBase<String, String>(database, TestLogger, storeScope) {
            override val definition = StubOperation
            override fun beforeEnqueue(input: String): String? = null
            override suspend fun perform(input: String): String = suspendForever()
        }
    )
}
