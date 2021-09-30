package me.sphere.appcore.usecases

import me.sphere.sqldelight.operations.OperationUtils
import me.sphere.sqldelight.operations.notifications.NotificationMarkAsReadOperation

internal fun createNotificationActionUseCase(
    operationUtils: OperationUtils
) = object : NotificationActionUseCase {
    override fun markAsRead(id: String) {
        operationUtils.enqueue(
            NotificationMarkAsReadOperation,
            NotificationMarkAsReadOperation.Input(id)
        )
    }
}