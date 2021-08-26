package me.sphere.appcore.firestore

import kotlinx.serialization.encoding.*

private const val SECONDS_KEY = "seconds"
private const val NANOSECONDS_KEY = "nanoseconds"

internal actual fun FirebaseDecoder.Companion.platformFallbackStructuralDecoder(value: Any): CompositeDecoder? {
    if (value::class.qualifiedName == "com.google.firebase.Timestamp") {
        return makeDecoder(value)
    }

    return null
}

private fun makeDecoder(timestamp: Any) = FirebaseClassDecoder(
    size = 2,
    containsKey = { it == SECONDS_KEY || it == NANOSECONDS_KEY }
) { descriptor, index ->
    val jClass = Class.forName("com.google.firebase.Timestamp")

    when (descriptor.getElementName(index)) {
        SECONDS_KEY -> jClass.getMethod("getSeconds").invoke(timestamp) as Long
        NANOSECONDS_KEY -> jClass.getMethod("getNanoseconds").invoke(timestamp) as Int
        else -> null
    }
}
