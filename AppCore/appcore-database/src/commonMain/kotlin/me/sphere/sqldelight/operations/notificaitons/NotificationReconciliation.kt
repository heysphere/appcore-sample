package me.sphere.sqldelight.operations.notificaitons

import me.sphere.sqldelight.operations.PagingReconciliationDefinition

object NotificationReconciliation : PagingReconciliationDefinition() {
    override val identifier: String
        get() = "NotificationPaging"
}