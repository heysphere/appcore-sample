package me.sphere.appcore.utils

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

interface Id<T> {
    val rawValue: T
}

open class LongIdSerializer<T: Id<Long>>(private val initializer: (Long) -> T): KSerializer<T> {
    override val descriptor: SerialDescriptor
        = PrimitiveSerialDescriptor("LongId", PrimitiveKind.LONG)

    init { freeze() }

    override fun deserialize(decoder: Decoder): T = initializer(decoder.decodeLong())
    override fun serialize(encoder: Encoder, value: T) = encoder.encodeLong(value.rawValue)
}
