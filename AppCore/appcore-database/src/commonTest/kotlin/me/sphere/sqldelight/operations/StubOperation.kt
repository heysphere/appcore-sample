package me.sphere.sqldelight.operations

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer

object StubOperation: OperationDefinition<String, String> {
    override val identifier: String = "Stub"

    override val inputSerializer: KSerializer<String> = String.serializer()
    override val outputSerializer: KSerializer<String> = String.serializer()
}

object UniquingStubOperation: OperationDefinition<UniquingStubOperation.Input, String> {
    @Serializable
    data class Input(val id: String, val content: String): DeduplicatingInput {
        override val deduplicationKey: String get() = id
    }

    override val identifier: String = "UniquingStub"

    override val inputSerializer: KSerializer<Input> = Input.serializer()
    override val outputSerializer: KSerializer<String> = String.serializer()
}
