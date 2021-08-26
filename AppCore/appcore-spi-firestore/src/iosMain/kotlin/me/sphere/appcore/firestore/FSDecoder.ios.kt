package me.sphere.appcore.firestore

import kotlinx.serialization.encoding.*
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSStringFromClass
import platform.Foundation.valueForKeyPath
import platform.darwin.NSObject

private const val SECONDS_KEY = "seconds"
private const val NANOSECONDS_KEY = "nanoseconds"

interface FirestoreTimestamp {
    val seconds: Long
    val nanoseconds: Int
}

internal actual fun FirebaseDecoder.Companion.platformFallbackStructuralDecoder(value: Any): CompositeDecoder? {
    if (value is FirestoreTimestamp) {
        return makeDecoder(value)
    }

    return null
}

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
private fun makeDecoder(timestamp: FirestoreTimestamp) = FirebaseClassDecoder(
    size = 2,
    containsKey = { it == SECONDS_KEY || it == NANOSECONDS_KEY }
) { descriptor, index ->
    when (descriptor.getElementName(index)) {
        SECONDS_KEY -> timestamp.seconds
        NANOSECONDS_KEY -> timestamp.nanoseconds
        else -> null
    }
}

