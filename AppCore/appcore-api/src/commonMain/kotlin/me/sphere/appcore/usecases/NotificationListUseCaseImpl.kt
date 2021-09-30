package me.sphere.appcore.usecases

import me.sphere.appcore.dataSource.PagingDataSource
import me.sphere.appcore.dataSource.pagingDataSource
import me.sphere.logging.Logger
import me.sphere.network.ConnectivityMonitor
import me.sphere.sqldelight.SqlDatabaseGateway
import me.sphere.sqldelight.StoreScope
import me.sphere.sqldelight.operations.OperationUtils
import me.sphere.sqldelight.operations.notifications.NotificationReconciliation

internal fun createNotificationListUseCase(
    database: SqlDatabaseGateway,
    operationUtils: OperationUtils,
    connectivityMonitor: ConnectivityMonitor,
    storeScope: StoreScope,
    logger: Logger,
) = object : NotificationListUseCase {
    override fun notifications(): PagingDataSource<Notification> = pagingDataSource(
        collectionKey = "notification",
        reconciliationOp = NotificationReconciliation,
        scope = storeScope.MainScope,
        database = database,
        pageSize = 40,
        operationUtils = operationUtils,
        getItem = { id ->
            database.notificationQueries.get(id)
        },
        mapper = { notification ->
            Notification(
                notification.id,
                unread = if (notification.optimisticRead) false else notification.unread,
                notification.title,
                notification.repositoryFullName,
                notification.subjectId
            )
        },
        logger = logger,
        connectivityMonitor = connectivityMonitor,
//        databaseUpdateTracking = DatabaseUpdateTracking(
//            getUpdateHead = TODO("Not sure what should this query do 🤔"),
//            getUpdatedRows = { database.notificationQueries.getNotificationMarkedOptimistically() },
//            getItemIdentifier = { item -> item.notificationId }
//        )
    )
}