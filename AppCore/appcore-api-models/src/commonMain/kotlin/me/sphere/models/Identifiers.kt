package me.sphere.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import me.sphere.appcore.utils.Id
import me.sphere.sqldelight.operations.DeduplicatingInput

/**
 * One single place to collect all identifier types.
 */

// TODO: Mark as inline/value classes when ObjC interop exports them properly.
@Suppress("unused")
@Serializable(with = TaggedString.Serializer::class)
data class TaggedString<out Tag>(override val rawValue: String): Id<String>, DeduplicatingInput {
    override val deduplicationKey: String get() = rawValue
    override fun toString(): String = rawValue

    companion object {
        @Suppress("UNCHECKED_CAST")
        inline fun <reified T> serializer(): KSerializer<TaggedString<T>> = Serializer as KSerializer<TaggedString<T>>
    }

    object Serializer: KSerializer<TaggedString<Nothing>> {
        override val descriptor = PrimitiveSerialDescriptor("TaggedString", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder) = TaggedString<Nothing>(decoder.decodeString())
        override fun serialize(encoder: Encoder, value: TaggedString<Nothing>)
            = encoder.encodeString(value.rawValue)
    }
}

// Typealiases are not exported to Objective-C. This avoids name conflicts with existing Swift tagged ID types.
typealias EmojiId = TaggedString<Tags.Emoji>
typealias AgentId = TaggedString<Tags.Agent>

object Tags {
    @Serializable(with = Serializer::class)
    object Agent

    @Serializable(with = Serializer::class)
    object Emoji

    object Serializer: KSerializer<Nothing> {
        override fun deserialize(decoder: Decoder): Nothing = error("Phantom tags cannot be deserialized")
        override fun serialize(encoder: Encoder, value: Nothing) = error("Phantom tags cannot be serialized")
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Tag", PrimitiveKind.STRING)
    }
}
