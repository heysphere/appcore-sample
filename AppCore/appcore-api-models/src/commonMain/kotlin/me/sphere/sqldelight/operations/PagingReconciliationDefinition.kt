package me.sphere.sqldelight.operations

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer

abstract class PagingReconciliationDefinition: OperationDefinition<PagingReconciliationDefinition.Input, PagingReconciliationDefinition.Output> {
    final override val inputSerializer = Input.serializer()
    final override val outputSerializer = Output.serializer()

    @Serializable
    data class Input(val collectionId: Long, val start: Long, val pageSize: Long): DeduplicatingInput {
        override val deduplicationKey: String get() = "$collectionId@$start+$pageSize"
    }

    @Serializable
    enum class Output {
        END_OF_COLLECTION, HAS_MORE;
    }
}
