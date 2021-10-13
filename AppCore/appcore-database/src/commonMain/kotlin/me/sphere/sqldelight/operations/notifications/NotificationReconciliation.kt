package me.sphere.sqldelight.operations.notifications

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import me.sphere.sqldelight.operations.PagingReconciliationDefinition

object NotificationReconciliation: PagingReconciliationDefinition<NotificationRequest>() {
    override val identifier = "NotificationPaging"

    override val payloadSerializer: KSerializer<NotificationRequest>
        get() = NotificationRequest.serializer()
}

@Serializable
data class NotificationRequest(
    val all: Boolean
)
