package me.sphere.sqldelight.operations

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

abstract class PagingReconciliationDefinition<Payload>: OperationDefinition<PagingReconciliationDefinition.Input<Payload>, PagingReconciliationDefinition.Output> {
    abstract val payloadSerializer: KSerializer<Payload>

    final override val inputSerializer: KSerializer<Input<Payload>>
        get() = Input.serializer(payloadSerializer)
    final override val outputSerializer = Output.serializer()

    @Serializable
    data class Input<Payload>(
        val collectionId: Long,
        val start: Long,
        val pageSize: Long,
        val payload: Payload
    )

    @Serializable
    enum class Output {
        END_OF_COLLECTION, HAS_MORE;
    }
}
