package me.sphere.appcore.firestore

import kotlinx.datetime.Instant
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.*

internal object InstantFirestoreSerializer: KSerializer<Instant> {
    @Serializable
    private class Surrogate(val seconds: Long, val nanoseconds: Int)

    override val descriptor: SerialDescriptor
        get() = Surrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): Instant = decoder
        .decodeSerializableValue(Surrogate.serializer())
        .let { Instant.fromEpochSeconds(it.seconds, it.nanoseconds) }

    override fun serialize(encoder: Encoder, value: Instant) = encoder
        .encodeSerializableValue(Surrogate.serializer(), Surrogate(value.epochSeconds, value.nanosecondsOfSecond))
}
