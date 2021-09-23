package me.sphere.appcore.usecases

import com.squareup.sqldelight.Query
import com.squareup.sqldelight.db.SqlCursor
import me.sphere.appcore.dataSource.PagingDataSource
import me.sphere.appcore.dataSource.pagingDataSource
import me.sphere.logging.Logger
import me.sphere.network.ConnectivityMonitor
import me.sphere.sqldelight.SqlDatabaseGateway
import me.sphere.sqldelight.StoreScope
import me.sphere.sqldelight.operations.OperationUtils
import me.sphere.sqldelight.operations.notificaitons.NotificationReconciliation

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
        getItem = {
            StubQuery()
        },
        mapper = {
            Notification("", false, "", "", "")
        },
        logger = logger,
        connectivityMonitor = connectivityMonitor
    )
}

private class StubQuery : Query<String>(mutableListOf(), {""}) {
    override fun <R> execute(mapper: (SqlCursor) -> R): R {
        TODO("FIX that with the real db implementation")
    }
}
