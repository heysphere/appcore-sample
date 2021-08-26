package me.sphere.appcore.utils

import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import okio.Buffer
import okio.ByteString.Companion.toByteString
import kotlin.math.sign
import kotlin.native.concurrent.SharedImmutable

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = Instant::class)
object InstantSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant = decoder.decodeString().let(Instant::parse)
    override fun serialize(encoder: Encoder, value: Instant) = value.toString().let(encoder::encodeString)
}

@SharedImmutable
val APPCORE_INSTANT_MIN = Instant.parse("0001-01-01T00:00:00.000000000Z")

@SharedImmutable
val APPCORE_INSTANT_MAX = Instant.parse("9999-12-31T23:59:59.999999999Z")

const val INSTANT_BLOB_SIZE = 1 + Long.SIZE_BYTES + Int.SIZE_BYTES

expect fun Instant.Companion.fromBlob(bytes: ByteArray): Instant
expect fun Instant.toBlob(): ByteArray

/**
 * A sign byte is added to achieve the ideal ASCII sort order (aka memcmp order).
 *
 * Integers in two's complement has the expected sort order when the signs are the same. However, from a bit layout
 * perspective, negative values in two's complement are ordered after positive values. So in order to achieve the sort
 * order we want, we insert a sign byte of `-` for negative values, and '0' for positive values.
 */
@OptIn(ExperimentalStdlibApi::class)
internal fun Instant.blobSignAsciiCode(): Int
    = if (epochSeconds.sign < 0) '-'.code else '0'.code

@OptIn(ExperimentalStdlibApi::class)
internal fun Instant.Companion.checkValidSignByte(byte: Byte) {
    check(byte == '-'.code.toByte() || byte == '0'.code.toByte())
}

internal fun Instant.Companion.fromBlobOkio(bytes: ByteArray): Instant = with(Buffer()) {
    check(bytes.size == INSTANT_BLOB_SIZE)
    write(bytes)
    checkValidSignByte(readByte())
    fromEpochSeconds(readLong(), readInt())
}

internal fun Instant.toBlobOkio(): ByteArray = with(Buffer()) {
    writeByte(blobSignAsciiCode())
    writeLong(epochSeconds)
    writeInt(nanosecondsOfSecond)
    check(size == INSTANT_BLOB_SIZE.toLong())
    readByteArray()
}
