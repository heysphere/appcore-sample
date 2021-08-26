package me.sphere.appcore.utils

import kotlinx.datetime.Instant

/**
 * Native implementations that are escape analysis friendly for heap-to-stack promotion.
 *
 * Little endian: [0:3] nsec [4:11] epoch [12] sign
 *    Big endian: [0] sign [1:8] epoch [9:12] nsec
 *
 * All iOS and Mac processors use little endian, while we want the blob format to be big endian to achieve the ideal
 * ASCII sort order.
 */

actual fun Instant.Companion.fromBlob(bytes: ByteArray): Instant {
    assert(Platform.isLittleEndian)

    // Assume incoming is big endian
    // Check sign byte
    checkValidSignByte(bytes[0])

    // Flip to be little endian
    val bufferLe = bytes.reversedArray()
    return fromEpochSeconds(bufferLe.getLongAt(Int.SIZE_BYTES), bufferLe.getIntAt(0))
}

actual fun Instant.toBlob(): ByteArray {
    assert(Platform.isLittleEndian)

    // Write in little endian
    val bufferLe = ByteArray(INSTANT_BLOB_SIZE)
    bufferLe.setIntAt(index = 0, value = nanosecondsOfSecond)
    bufferLe.setLongAt(index = Int.SIZE_BYTES, value = epochSeconds)
    bufferLe.set(index = INSTANT_BLOB_SIZE - 1, value = blobSignAsciiCode().toByte())

    // Flip to be big endian
    return bufferLe.reversedArray()
}
