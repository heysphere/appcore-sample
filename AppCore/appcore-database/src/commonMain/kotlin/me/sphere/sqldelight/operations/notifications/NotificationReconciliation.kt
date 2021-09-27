package me.sphere.sqldelight.operations.notifications

import me.sphere.sqldelight.operations.PagingReconciliationDefinition

object NotificationReconciliation : PagingReconciliationDefinition() {
    override val identifier: String
        get() = "NotificationPaging"
}