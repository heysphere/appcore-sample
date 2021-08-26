package me.sphere.appcore

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withTimeout
import me.sphere.appcore.dataSource.DataSource
import me.sphere.appcore.dataSource.ListDataSource
import me.sphere.appcore.dataSource.listDataSource
import me.sphere.appcore.dataSource.singleDataSource
import me.sphere.appcore.usecases.EditProfile
import me.sphere.appcore.utils.Atomic
import me.sphere.appcore.utils.freeze
import me.sphere.logging.Logger
import me.sphere.sqldelight.MyAgent
import me.sphere.sqldelight.StoreScope
import me.sphere.sqldelight.SqlDatabaseGateway
import me.sphere.sqldelight.operations.GetMyAgentOperation
import me.sphere.sqldelight.operations.OperationStoreActorBase
import me.sphere.sqldelight.operations.OperationUtils
import me.sphere.sqldelight.operations.EmptyInput
import me.sphere.test.DbTests
import me.sphere.test.support.TestLogger
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

/**
 * To avoid setting up a stub schema these test is relying on the [MyAgent] table.
 */

internal class SingleDataSourceTest : DataSourceTest<EditProfile.Agent, DataSource<EditProfile.Agent>>() {
    override fun makeDataSource() = singleDataSource(
        operationUtils = operationUtils,
        operation = GetMyAgentOperation,
        input = EmptyInput,
        query = database.myAgentQueries.getMyAgent(),
        mapper = agentMapper()
    )

    override fun upsertEntity(testData: String) = database.myAgentQueries.upsert(myAgent(testData))
    override fun expectedValue(testData: String) = DataSource.State.Value(agent(testData))
}

internal class ListDataSourceTest : DataSourceTest<List<EditProfile.Agent>, ListDataSource<EditProfile.Agent>>() {
    override fun makeDataSource() = listDataSource(
        operationUtils = operationUtils,
        operation = GetMyAgentOperation,
        input = EmptyInput,
        query = database.myAgentQueries.getMyAgent(),
        mapper = agentMapper()
    )

    override fun upsertEntity(testData: String) = database.myAgentQueries.upsert(myAgent(testData))
    override fun expectedValue(testData: String) = DataSource.State.Value(listOf(agent(testData)))

}

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
internal abstract class DataSourceTest<Type: Any, Source : DataSource<Type>> : DbTests() {
    protected val operationUtils = OperationUtils(database, TestLogger, StoreScope)
    private val actor = StubActor(database, TestLogger, StoreScope)

    abstract fun makeDataSource(): Source
    abstract fun upsertEntity(testData: String = "original")
    abstract fun expectedValue(testData: String = "original"): DataSource.State.Value<Type>

    @BeforeTest
    fun setup() {
        // WHY do I need this but not present in:
        StoreScope.register(listOf(actor))
        actor.attach()
    }


    @Test
    fun initialState() = runTesting {
        makeDataSource().state.test {
            assertEquals(DataSource.State.Loading, expectItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun initialStateWithData() = runTesting {
        // Make the actor suspends forever, so if `DataSource.state` did not serve the data immediately, it will
        // definitely fail the 1 second timeout.
        actor.action.value = { suspendForever() }

        upsertEntity()

        // Test that it is delivered immediately.
        val initial = withTimeout(1.seconds) {
            makeDataSource().state.first()
        }

        assertEquals(expectedValue(), actual = initial)
    }


    @Test
    fun loadSuccess() = runTesting {
        actor.action.value = {
            upsertEntity()
        }

        makeDataSource().state.test {
            assertEquals(DataSource.State.Loading, expectItem())
            assertEquals(expectedValue(), expectItem())
        }
    }

    @Test
    fun loadFailed() = runTesting {
        actor.action.value = {
            throw Exception()
        }

        makeDataSource().state.test {
            assertEquals(DataSource.State.Loading, expectItem())
            val item = expectItem()
            assertTrue { item is DataSource.State.Failed }
        }
    }

    @Test
    fun loadFailedThenRetry() = runTesting {
        actor.action.value = {
            throw Exception()
        }

        val source = makeDataSource()
        source.state.test {
            assertEquals(DataSource.State.Loading, expectItem())
            val item = expectItem()
            assertTrue { item is DataSource.State.Failed }

            val semaphore = Semaphore(1, 1)
            actor.action.value = {
                semaphore.acquire()
                upsertEntity()
            }
            source.refreshNow()

            assertEquals(DataSource.State.Loading, expectItem())
            semaphore.release()

            assertEquals(expectedValue(), expectItem())
        }
    }

    @Test
    fun multipleUpdates() = runTesting {
        upsertEntity()
        makeDataSource().state.test {
            assertEquals(expectedValue(), expectItem())

            upsertEntity("updated")
            assertEquals(expectedValue("updated"), expectItem())
        }
    }

    @Test
    fun refresh() = runTesting {
        upsertEntity()

        val source = makeDataSource()
        source.state.test {
            assertEquals(expectedValue(), expectItem())

            actor.action.value = {
                upsertEntity("updated")
            }
            source.refreshNow()

            assertEquals(expectedValue("updated"), expectItem())
        }
    }

    @Test
    fun testMultipleCallRetry() = runTesting {
        actor.action.value = {
            throw Exception()
        }

        makeDataSource().state.test {
            assertEquals(DataSource.State.Loading, expectItem())
            val item = expectItem()
            assertTrue { item is DataSource.State.Failed }

            val semaphore = Semaphore(1, 1)
            actor.action.value = {
                /**
                 * Suspend until our flow asserter below sees [AbstractDataSource.State.Loading].
                 */
                semaphore.acquire()

                upsertEntity()
            }

            /**
             * Create another instance of the data source, and trigger [AbstractDataSource.refreshNow]. All instances pointing
             * to the same object or the same object collection should behave as if they are the same.
             */
            makeDataSource().refreshNow()

            assertEquals(DataSource.State.Loading, expectItem())
            semaphore.release()

            assertEquals(expectedValue(), expectItem())
        }
    }
}

private class StubActor(
    database: SqlDatabaseGateway,
    logger: Logger,
    StoreScope: StoreScope
) : OperationStoreActorBase<EmptyInput, Unit>(
    database,
    logger,
    StoreScope
) {
    private val emptyAction: suspend (EmptyInput) -> Unit = {}
    val action = Atomic<(suspend (EmptyInput) -> Unit)?>(
        emptyAction.freeze()
    )

    override val definition = GetMyAgentOperation

    init {
        freeze()
    }

    override suspend fun perform(input: EmptyInput) {
        return action.value?.invoke(input) ?: throw IllegalStateException("Null input")
    }
}

private fun agentMapper() = { agent: MyAgent ->
    EditProfile.Agent(
        id = agent.id,
        name = agent.name,
        about = agent.about,
        imageColor = agent.imageColor,
        imageFileName = agent.imageFileName,
    )
}

private fun myAgent(testData: String) = MyAgent(
    id = "id",
    name = "name",
    imageColor = "imageColor",
    imageFileName = "imageFileName",
    about = testData,
)

private fun agent(testData: String) = EditProfile.Agent(
    id = "id",
    name = "name",
    imageColor = "imageColor",
    imageFileName = "imageFileName",
    about = testData
)
