package me.sphere.appcore

import me.sphere.appcore.usecases.*
import me.sphere.appcore.usecases.createNotificationInfoUseCase
import me.sphere.appcore.usecases.createNotificationListUseCase
import me.sphere.appcore.utils.freeze
import me.sphere.logging.Logger
import me.sphere.network.ConnectivityMonitor
import me.sphere.sqldelight.*

data class SphereStoreBuilder(
    val storeActorBuilders: List<StoreActorBuilder>,
    val sqlDatabaseProvider: SqlDatabaseProvider,
    val preferences: PreferenceStore,
    val connectivityMonitor: ConnectivityMonitor,
    val logger: Logger
)

fun SphereStoreBuilder.makeStore(
    gitHubAccessToken: String,
): SphereStore {
    return object : ClientBase(
        StoreClientType.App,
        gitHubAccessToken,
        storeActorBuilders,
        sqlDatabaseProvider,
        logger
    ), SphereStore {

        override val notificationListUseCase: NotificationListUseCase = createNotificationListUseCase(
            database,
            operationUtils,
            connectivityMonitor,
            storeScope,
            logger
        )
        override val notificationInfoUseCase: NotificationInfoUseCase = createNotificationInfoUseCase(
            database,
            operationUtils
        )
        override val notificationActionUseCase: NotificationActionUseCase =
            createNotificationActionUseCase(operationUtils)

        init {
            freeze()
        }

        override fun destroy() {
            close()
            sqlDatabaseProvider.destroy(databaseName)
        }
    }
}
