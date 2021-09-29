package me.sphere.sqldelight.operations.notifications

import kotlinx.serialization.Serializable
import me.sphere.sqldelight.operations.DeduplicatingInput
import me.sphere.sqldelight.operations.OperationDefinition

object FetchNotificationInfoOperation: OperationDefinition<FetchNotificationInfoOperation.Input, FetchNotificationInfoOperation.Output> {
    @Serializable
    data class Input(
        val notificationId: String,
    ): DeduplicatingInput {
        override val deduplicationKey = notificationId
    }

    @Serializable
    data class Output(
        val notificationId: String
    )

    override val identifier = "FetchNotificationInfoOperation"
    override val inputSerializer = Input.serializer()
    override val outputSerializer = Output.serializer()
}
