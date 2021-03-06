package me.sphere.appcore

import kotlinx.coroutines.SupervisorJob
import me.sphere.logging.Logger
import me.sphere.sqldelight.*
import me.sphere.sqldelight.operations.OperationUtils

abstract class ClientBase(
    clientType: StoreClientType,
    gitHubAccessToken: String,
    storeActorBuilders: List<StoreActorBuilder>,
    sqlDatabaseProvider: SqlDatabaseProvider,
    logger: Logger
) {
    val isActive: Boolean
        get() = superviserJob.isActive

    protected val database: SqlDatabaseGateway
    protected val operationUtils: OperationUtils
    protected val storeScope: StoreScope
    protected val databaseName = SQLCORE_SCHEMA_HASH

    private val superviserJob = SupervisorJob()

    init {
        val (database, handle) = sqlDatabaseProvider.loadOrCreate(databaseName, clientType)
        this.database = database
        this.storeScope = StoreScope(
            gitHubAccessToken,
            clientType,
            database.managedOperationQueries,
            logger
        )

        val actors = storeActorBuilders.flatMap { it.build(database, storeScope) }
        storeScope.register(actors)
        operationUtils = OperationUtils(database, logger, storeScope)

        superviserJob.invokeOnCompletion {
            storeScope.close()
            handle.close()
        }

        for (actor in actors) {
            /**
             * Actors are expected to launch their [Flow] using coroutine scopes provided by [SphereStoreScope].
             * So in terms of cancellation, they are tracked by [SphereStoreScope] already, and does not require
             * other explicit means.
             */
            actor.attach()
        }
    }

    fun close() { superviserJob.cancel() }
}
