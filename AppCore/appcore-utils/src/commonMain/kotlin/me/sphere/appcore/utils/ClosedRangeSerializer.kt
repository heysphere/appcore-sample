package me.sphere.appcore.utils

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

private const val START_SERIAL_NAME = "s"
private const val ENDINCLUSIVE_SERIAL_NAME = "ei"

@OptIn(ExperimentalSerializationApi::class)
open class ClosedRangeSerializer<T: Comparable<T>>(private val boundSerializer: KSerializer<T>): KSerializer<ClosedRange<T>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ClosedRange", boundSerializer.descriptor) {
        element(START_SERIAL_NAME, descriptor = boundSerializer.descriptor)
        element(ENDINCLUSIVE_SERIAL_NAME, descriptor = boundSerializer.descriptor)
    }

    override fun deserialize(decoder: Decoder): ClosedRange<T> {
        return decoder.decodeStructure(descriptor) {
            var lower: T? = null
            var upper: T? = null

            while (true) {
                val index = decodeElementIndex(descriptor)
                if (index == CompositeDecoder.DECODE_DONE) { break }

                val value = this.decodeSerializableElement(descriptor, index, boundSerializer)
                when (descriptor.getElementName(index)) {
                    START_SERIAL_NAME -> { lower = value }
                    ENDINCLUSIVE_SERIAL_NAME -> { upper = value }
                }
            }

            check(lower != null) { "Lower bound (key = `$START_SERIAL_NAME`) is missing from closed range." }
            check(upper != null) { "Upper bound (key = `$ENDINCLUSIVE_SERIAL_NAME`) is missing from closed range." }
            lower .. upper
        }
    }

    override fun serialize(encoder: Encoder, value: ClosedRange<T>) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, descriptor.getElementIndex(START_SERIAL_NAME), boundSerializer, value.start)
            encodeSerializableElement(descriptor, descriptor.getElementIndex(ENDINCLUSIVE_SERIAL_NAME), boundSerializer, value.endInclusive)
        }
    }
}
