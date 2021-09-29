package me.sphere.sqldelight.operations.notifications

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import me.sphere.sqldelight.operations.DeduplicatingInput
import me.sphere.sqldelight.operations.OperationDefinition

object FetchNotificationInfoOperation: OperationDefinition<FetchNotificationInfoOperation.Input, Unit> {
    @Serializable
    data class Input(
        val notificationId: String,
    ): DeduplicatingInput {
        override val deduplicationKey = notificationId
    }

    override val identifier = "FetchNotificationInfoOperation"
    override val inputSerializer = Input.serializer()
    override val outputSerializer = Unit.serializer()
}
