package me.sphere.appcore.utils

fun <T: Comparable<T>> ClosedRange<T>.intersects(other: ClosedRange<T>): Boolean
    = other.start <= endInclusive && start <= other.endInclusive

/**
 * Return the union of [this] and [other], even when they are not intersecting each other.
 *
 * In other words, the return range is a range that includes starts at the minimum [ClosedRange.start] of the two, and
 * ends inclusively at the maximum [ClosedRange.endInclusive] of the two.
 */
operator fun <T: Comparable<T>> ClosedRange<T>.plus(other: ClosedRange<T>): ClosedRange<T>
    = minOf(start, other.start) .. maxOf(endInclusive, other.endInclusive)

fun <T: Comparable<T>, U: Comparable<U>> ClosedRange<T>.elementwiseMap(transform: (T) -> U): ClosedRange<U>
    = transform(start) .. transform(endInclusive)
