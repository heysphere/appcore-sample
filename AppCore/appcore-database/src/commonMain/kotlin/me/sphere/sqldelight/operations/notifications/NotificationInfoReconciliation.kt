package me.sphere.sqldelight.operations.notifications

import me.sphere.sqldelight.operations.PagingReconciliationDefinition

object NotificationInfoReconciliation: PagingReconciliationDefinition() {
    override val identifier = "NotificationInfoPaging"
}
