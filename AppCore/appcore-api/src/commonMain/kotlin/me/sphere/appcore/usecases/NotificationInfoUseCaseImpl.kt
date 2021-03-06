package me.sphere.appcore.usecases

import me.sphere.appcore.dataSource.DataSource
import me.sphere.appcore.dataSource.singleDataSource
import me.sphere.logging.Logger
import me.sphere.network.ConnectivityMonitor
import me.sphere.sqldelight.SqlDatabaseGateway
import me.sphere.sqldelight.StoreScope
import me.sphere.sqldelight.operations.OperationUtils
import me.sphere.sqldelight.operations.notifications.FetchNotificationInfoOperation

internal fun createNotificationInfoUseCase(
    database: SqlDatabaseGateway,
    operationUtils: OperationUtils,
) = object: NotificationInfoUseCase {
    override fun info(id: String): DataSource<NotificationInfo> = singleDataSource(
        operationUtils = operationUtils,
        operation = FetchNotificationInfoOperation,
        input = FetchNotificationInfoOperation.Input(
            notificationId = id
        ),
        query = database.notificationQueries.get(id),
        mapper = { notification ->
            NotificationInfo(
                notification.id,
                notification.reason,
                notification.title,
                notification.repositoryFullName,
                notification.subjectId
            )
        }
    )
}
