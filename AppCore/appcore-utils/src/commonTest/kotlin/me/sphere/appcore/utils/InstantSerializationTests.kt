package me.sphere.appcore.utils

import kotlinx.datetime.*
import kotlin.math.sign
import kotlin.test.*

@kotlin.ExperimentalUnsignedTypes
class InstantSerializationTests {
    private val minSecMinNsec = Instant.DISTANT_PAST.plus(-999_999_999, DateTimeUnit.NANOSECOND)
    private val minSecMaxNsec = Instant.DISTANT_PAST
    private val zeroSecMinNsec = Instant.fromEpochSeconds(0, 0)
    private val zeroSecMaxNsec = Instant.fromEpochSeconds(0, 999_999_999)
    private val maxSecMinNsec = Instant.DISTANT_FUTURE
    private val maxSecMaxNsec = Instant.DISTANT_FUTURE.plus(999_999_999, DateTimeUnit.NANOSECOND)

    @Test
    fun test_constants() {
        /**
         * These must match the valid range of Firestore timestamps
         *
         * References:
         * https://github.com/protocolbuffers/protobuf/blob/c47ebfd82be3d07be4f003e2c34c7c86b5d4fced/src/google/protobuf/timestamp.proto#L136-L147
         * https://github.com/firebase/firebase-ios-sdk/blob/1e39eb4cff55d631feee87f79118ee45a94982fd/Firestore/Source/API/FIRTimestamp.m#L68-L94
         */
        assertEquals(APPCORE_INSTANT_MIN.epochSeconds, -62135596800L)
        assertEquals(APPCORE_INSTANT_MIN.nanosecondsOfSecond, 0)
        assertEquals(APPCORE_INSTANT_MAX.epochSeconds, 253402300799L)
        assertEquals(APPCORE_INSTANT_MAX.nanosecondsOfSecond, 999999999)
    }

    @Test
    @ExperimentalStdlibApi
    fun test_comparison() {
        assertMemcmpOrder(minSecMinNsec, minSecMaxNsec, Order.Ascending)
        assertMemcmpOrder(minSecMaxNsec, minSecMinNsec, Order.Descending)
        assertMemcmpOrder(minSecMinNsec, minSecMinNsec, Order.Equal)
        assertMemcmpOrder(minSecMaxNsec, minSecMaxNsec, Order.Equal)

        assertMemcmpOrder(zeroSecMinNsec, zeroSecMaxNsec, Order.Ascending)
        assertMemcmpOrder(zeroSecMaxNsec, zeroSecMinNsec, Order.Descending)
        assertMemcmpOrder(zeroSecMinNsec, zeroSecMinNsec, Order.Equal)
        assertMemcmpOrder(zeroSecMaxNsec, zeroSecMaxNsec, Order.Equal)

        assertMemcmpOrder(maxSecMinNsec, maxSecMaxNsec, Order.Ascending)
        assertMemcmpOrder(maxSecMaxNsec, maxSecMinNsec, Order.Descending)
        assertMemcmpOrder(maxSecMinNsec, maxSecMinNsec, Order.Equal)
        assertMemcmpOrder(maxSecMaxNsec, maxSecMaxNsec, Order.Equal)

        assertMemcmpOrder(minSecMinNsec, maxSecMaxNsec, Order.Ascending)
        assertMemcmpOrder(maxSecMaxNsec, minSecMinNsec, Order.Descending)
        assertMemcmpOrder(minSecMinNsec, zeroSecMaxNsec, Order.Ascending)
        assertMemcmpOrder(zeroSecMaxNsec, minSecMinNsec, Order.Descending)
        assertMemcmpOrder(zeroSecMinNsec, maxSecMaxNsec, Order.Ascending)
        assertMemcmpOrder(maxSecMaxNsec, zeroSecMinNsec, Order.Descending)
    }

    @Test
    fun test_roundtrip() {
        val ts = Instant.fromEpochSeconds(1024768, 999888777)
        val data = ts.toBlob()
        val reconstructed = Instant.fromBlob(data)

        assertEquals(reconstructed, ts)
    }
}

@ExperimentalStdlibApi
@kotlin.ExperimentalUnsignedTypes
private fun assertMemcmpOrder(lhs: Instant, rhs: Instant, order: Order) {
    val rawLhs = lhs.toBlob()
    val rawRhs = rhs.toBlob()

    assertEquals(rawLhs.count(), rawRhs.count())

    val comparisons = rawLhs
        .zip(rawRhs)
        .map { compareValues(it.first, it.second) }
        .firstOrNull { it != 0 }

    when (comparisons?.sign ?: 0) {
        -1 -> assertEquals(order.rawValue, -1)
        0 -> assertEquals(order.rawValue, 0)
        1 -> assertEquals(order.rawValue, 1)
        else -> fail("Unidentified value")
    }
}

private enum class Order(val rawValue: Int) {
    // lhs < rhs
    Ascending(-1),

    // lhs == rhs
    Equal(0),

    // lhs > rhs
    Descending(1)
}
