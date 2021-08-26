package me.sphere.appcore.firestore

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

// Adapted from https://github.com/GitLiveApp/firebase-kotlin-sdk/tree/6adf9f1f5709ea297d85778015afbeccb2979501/firebase-common
// Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
@PublishedApi
internal inline fun <reified T> firestoreDecode(strategy: DeserializationStrategy<T>, value: Any, reference: DocumentReference): T {
    try {
        return FirebaseTopLevelDecoder(value, reference).decodeSerializableValue(strategy)
    } catch (error: Throwable) {
        // Wrap any decoding exception as the cause to `FirestoreDecodeException`, so that we can log extra information
        // about the exception, e.g., the document path.
        throw FirestoreDecodeException(reference, T::class, error)
    }
}

internal expect fun FirebaseDecoder.Companion.platformFallbackStructuralDecoder(value: Any): CompositeDecoder?

@PublishedApi
internal class FirebaseTopLevelDecoder(private val value: Any?, reference: DocumentReference): FirebaseDecoder(value) {
    override val extras: Map<String, Any> = mapOf(DOCUMENT_ID_NAME to reference.documentID)
}

@OptIn(ExperimentalSerializationApi::class)
internal open class FirebaseDecoder(private val value: Any?, private val isInline: Boolean = false) : Decoder {
    companion object {}

    open val extras: Map<String, Any> = emptyMap()
    override val serializersModule = EmptySerializersModule

    @Suppress("UNCHECKED_CAST")
    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        check(!isInline)
        require(value != null) {
            if (descriptor.isNullable) {
                """
                `${descriptor.serialName}` is null, but somehow `beginStructure()` is called instead of `decodeNull()`.
                This could be a Kotlin Serialization bug.
                """.trimIndent()
            } else {
                """
                `${descriptor.serialName}` is null, but the associated property was declared as non-nullable.
                """.trimIndent()
            }
        }

        return when (descriptor.kind as StructureKind) {
            StructureKind.CLASS, StructureKind.OBJECT -> {
                when (value) {
                    is Map<*, *> -> makeMapDecoder(value, extras)
                    null -> makeEmptyDecoder()
                    else -> platformFallbackStructuralDecoder(value) ?: makeEmptyDecoder()
                }
            }
            StructureKind.LIST -> (value as List<*>).let {
                FirebaseCompositeDecoder(it.size) { _, index -> it[index] }
            }
            StructureKind.MAP -> (value as Map<*, *>).entries.toList().let {
                FirebaseCompositeDecoder(it.size) { _, index -> it[index / 2].run { if (index % 2 == 0) key else value } }
            }
        }
    }

    override fun decodeString() = decodeString(value)
    override fun decodeDouble() = decodeDouble(value)
    override fun decodeLong() = decodeLong(value)
    override fun decodeByte() = decodeByte(value)
    override fun decodeFloat() = decodeFloat(value)
    override fun decodeInt() = decodeInt(value)
    override fun decodeShort() = decodeShort(value)
    override fun decodeBoolean() = decodeBoolean(value)
    override fun decodeChar() = decodeChar(value)
    override fun decodeEnum(enumDescriptor: SerialDescriptor) = decodeEnum(value, enumDescriptor)
    override fun decodeNotNullMark() = decodeNotNullMark(value)
    override fun decodeNull() = decodeNull(value)

    override fun decodeInline(inlineDescriptor: SerialDescriptor): Decoder = FirebaseDecoder(value, isInline = true)
}

@OptIn(ExperimentalSerializationApi::class)
internal class FirebaseClassDecoder(
    size: Int,
    private val containsKey: (name: String) -> Boolean,
    get: (descriptor: SerialDescriptor, index: Int) -> Any?
) : FirebaseCompositeDecoder(size, get) {
    private var index: Int = 0

    override fun decodeSequentially() = false

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (index == descriptor.elementsCount) {
            return CompositeDecoder.DECODE_DONE
        }

        val nextIndex = index
        index += 1

        val elementDescriptor = descriptor.getElementDescriptor(nextIndex)
        val elementName = descriptor.getElementName(nextIndex)
        val hasKey = containsKey(elementName)

        require(descriptor.isElementOptional(nextIndex) || hasKey) {
            val nullability = if (elementDescriptor.isNullable) "nullable" else "non-nullable"
            "Expected $nullability element `$elementName` to be present. Found no trace of its existence."
        }

        return nextIndex
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal open class FirebaseCompositeDecoder constructor(
    private val size: Int,
    private val get: (descriptor: SerialDescriptor, index: Int) -> Any?
) : CompositeDecoder {
    override val serializersModule = EmptySerializersModule

    override fun decodeSequentially() = true
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = throw NotImplementedError()
    override fun decodeCollectionSize(descriptor: SerialDescriptor) = size

    override fun <T> decodeSerializableElement(descriptor: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T>, previousValue: T?): T
        = deserializer.deserialize(FirebaseDecoder(get(descriptor, index)))

    override fun <T : Any> decodeNullableSerializableElement(descriptor: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T?>, previousValue: T?): T? = when {
        decodeNotNullMark(get(descriptor, index)) -> decodeSerializableElement(descriptor, index, deserializer)
        else -> decodeNull(get(descriptor, index))
    }

    override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int)
        = decodeBoolean(checkNullability(descriptor, get(descriptor, index)))
    override fun decodeByteElement(descriptor: SerialDescriptor, index: Int)
        = decodeByte(checkNullability(descriptor, get(descriptor, index)))
    override fun decodeCharElement(descriptor: SerialDescriptor, index: Int)
        = decodeChar(checkNullability(descriptor, get(descriptor, index)))
    override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int)
        = decodeDouble(checkNullability(descriptor, get(descriptor, index)))
    override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int)
        = decodeFloat(checkNullability(descriptor, get(descriptor, index)))
    override fun decodeIntElement(descriptor: SerialDescriptor, index: Int)
        = decodeInt(checkNullability(descriptor, get(descriptor, index)))
    override fun decodeLongElement(descriptor: SerialDescriptor, index: Int)
        = decodeLong(checkNullability(descriptor, get(descriptor, index)))
    override fun decodeShortElement(descriptor: SerialDescriptor, index: Int)
        = decodeShort(checkNullability(descriptor, get(descriptor, index)))
    override fun decodeStringElement(descriptor: SerialDescriptor, index: Int)
        = decodeString(checkNullability(descriptor, get(descriptor, index)))
    override fun endStructure(descriptor: SerialDescriptor) {}

    override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder
        = FirebaseDecoder(checkNullability(descriptor, get(descriptor, index)), isInline = true)
}

@OptIn(ExperimentalSerializationApi::class)
private fun checkNullability(descriptor: SerialDescriptor, value: Any?): Any? {
    require(descriptor.isNullable || value != null) {
        "Expected ${descriptor.serialName} to be non-null. Found null."
    }

    return value
}

private fun decodeString(value: Any?) = value.toString()

private fun decodeDouble(value: Any?) = when (value) {
    is Number -> value.toDouble()
    is String -> value.toDouble()
    else -> throw SerializationException("Expected $value to be double")
}

private fun decodeLong(value: Any?) = when (value) {
    is Number -> value.toLong()
    is String -> value.toLong()
    else -> throw SerializationException("Expected $value to be long")
}

private fun decodeByte(value: Any?) = when (value) {
    is Number -> value.toByte()
    is String -> value.toByte()
    else -> throw SerializationException("Expected $value to be byte")
}

private fun decodeFloat(value: Any?) = when (value) {
    is Number -> value.toFloat()
    is String -> value.toFloat()
    else -> throw SerializationException("Expected $value to be float")
}

private fun decodeInt(value: Any?) = when (value) {
    is Number -> value.toInt()
    is String -> value.toInt()
    else -> throw SerializationException("Expected $value to be int")
}

private fun decodeShort(value: Any?) = when (value) {
    is Number -> value.toShort()
    is String -> value.toShort()
    else -> throw SerializationException("Expected $value to be short")
}

private fun decodeBoolean(value: Any?) = value as Boolean

private fun decodeChar(value: Any?) = when (value) {
    is Number -> value.toChar()
    is String -> value[0]
    else -> throw SerializationException("Expected $value to be char")
}

@OptIn(ExperimentalSerializationApi::class)
private fun decodeEnum(value: Any?, enumDescriptor: SerialDescriptor) = when (value) {
    is Number -> value.toInt()
    is String -> enumDescriptor.getElementIndex(value)
    else -> throw SerializationException("Expected $value to be enum")
}

private fun decodeNotNullMark(value: Any?) = value != null
private fun decodeNull(value: Any?) = value as Nothing?

private fun makeEmptyDecoder() = FirebaseCompositeDecoder(0) { _, _ -> }

@OptIn(ExperimentalSerializationApi::class)
private fun makeMapDecoder(values: Map<*, *>, extras: Map<String, *>) = FirebaseClassDecoder(
    size = values.count(),
    containsKey = { values.containsKey(it) || extras.containsKey(it) }
) { descriptor, index ->
    val name = descriptor.getElementName(index)
    values[name] ?: extras[name]
}
