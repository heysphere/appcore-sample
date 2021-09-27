package me.sphere.test

import kotlinx.coroutines.CoroutineDispatcher
import me.sphere.appcore.utils.uuid
import me.sphere.models.AgentId
import me.sphere.sqldelight.*
import me.sphere.sqldelight.operations.OperationUtils
import me.sphere.test.support.InMemorySqlDatabaseProvider
import me.sphere.test.support.TestLogger
import me.sphere.test.support.runTesting as runTestingImpl
import kotlin.test.AfterTest

@OptIn(ExperimentalStdlibApi::class)
abstract class DbTests: Tests() {
    private val databaseHandle = InMemorySqlDatabaseProvider.loadOrCreate(uuid(), StoreClientType.App)

    @Deprecated("Use the StoreScope given by `runTesting`, which runs actors on the same event loop as the test cases.")
    val StoreScope = StoreScope(AgentId("stub"), StoreClientType.App, BackendEnvironmentType.TESTING, database.managedOperationQueries, TestLogger)

    val database: SqlDatabaseGateway
        get() = databaseHandle.first

    @AfterTest
    override fun cleanup() {
        super.cleanup()
        StoreScope.close()
        databaseHandle.second.close()
    }

    fun runTesting(body: suspend Scope.() -> Unit) = runTestingImpl {
        val dispatcher = coroutineContext[CoroutineDispatcher.Key]!!
        val scope = Scope(database, dispatcher)

        scope.body()

        /**
         * Complete this test run only after all coroutines in the AppCore system have been cancelled or has completed.
         * 
         * This eliminates the SQLite Exceptions raised due to [databaseHandle] closure and [StoreScope] closure
         * not being sequential, since now [cleanup] must run after [StoreScope] finishes cancelling everything.
         */
        scope.storeScope.closeAndJoin()
        StoreScope.closeAndJoin()
    }

    class Scope(database: SqlDatabaseGateway, dispatcher: CoroutineDispatcher) {
        val storeScope = StoreScope(
            AgentId("stub"),
            StoreClientType.App,
            BackendEnvironmentType.TESTING,
            database.managedOperationQueries,
            TestLogger,
            overrideDispatcher = dispatcher
        )
        val operationUtils = OperationUtils(database, TestLogger, storeScope)
    }
}

