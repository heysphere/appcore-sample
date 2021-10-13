package me.sphere.appcore.usecases

import me.sphere.appcore.dataSource.DatabaseUpdateTracking
import me.sphere.appcore.dataSource.PagingDataSource
import me.sphere.appcore.dataSource.pagingDataSource
import me.sphere.logging.Logger
import me.sphere.models.InstantColumnAdapter
import me.sphere.network.ConnectivityMonitor
import me.sphere.sqldelight.SqlDatabaseGateway
import me.sphere.sqldelight.StoreScope
import me.sphere.sqldelight.operations.OperationUtils
import me.sphere.sqldelight.operations.notifications.NotificationReconciliation
import kotlinx.datetime.Instant
import me.sphere.sqldelight.operations.notifications.NotificationRequest

internal fun createNotificationListUseCase(
    database: SqlDatabaseGateway,
    operationUtils: OperationUtils,
    connectivityMonitor: ConnectivityMonitor,
    storeScope: StoreScope,
    logger: Logger,
) = object : NotificationListUseCase {
    override fun notifications(shouldShowAll: Boolean): PagingDataSource<Notification> = pagingDataSource(
        collectionKey = "notification",
        reconciliationOp = NotificationReconciliation,
        scope = storeScope.MainScope,
        database = database,
        pageSize = 40,
        payload = NotificationRequest(shouldShowAll),
        operationUtils = operationUtils,
        getItem = { id ->
            if (shouldShowAll) {
                database.notificationQueries.get(id)
            } else {
                database.notificationQueries.getUnread(id)
            }
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
        databaseUpdateTracking = DatabaseUpdateTracking(
            getUpdateHead = database.notificationQueries.getNotificationUpdateHead { it ->
                it?.let { InstantColumnAdapter.decode(it) } ?: Instant.DISTANT_PAST
            },
            getUpdatedRows = { database.notificationQueries.getNotificationMarkedOptimistically() },
            getItemIdentifier = { item -> item.notificationId }
        )
    )
}
