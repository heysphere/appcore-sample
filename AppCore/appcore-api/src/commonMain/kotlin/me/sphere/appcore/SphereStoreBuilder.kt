package me.sphere.appcore

import me.sphere.appcore.utils.freeze
import me.sphere.logging.Logger
import me.sphere.models.AgentId
import me.sphere.models.BackendEnvironmentType
import me.sphere.network.ConnectivityMonitor
import me.sphere.sqldelight.*

data class SphereStoreBuilder(
    val environmentType: BackendEnvironmentType,
    val storeActorBuilders: List<StoreActorBuilder>,
    val sqlDatabaseProvider: SqlDatabaseProvider,
    val preferences: PreferenceStore,
    val connectivityMonitor: ConnectivityMonitor,
    val logger: Logger
)

fun SphereStoreBuilder.makeStore(
    agentId: AgentId
): SphereStore {
    return object : ClientBase(
        StoreClientType.App,
        environmentType,
        agentId,
        storeActorBuilders,
        sqlDatabaseProvider,
        logger
    ), SphereStore {
        init {
            freeze()
        }

        override fun destroy() {
            close()
            sqlDatabaseProvider.destroy(databaseName)
        }
    }
}

