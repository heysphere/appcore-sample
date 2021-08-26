package me.sphere.test.support

import com.squareup.sqldelight.internal.Atomic
import me.sphere.appcore.utils.freeze

fun <V> Atomic<V>.setFrozen(value: V) {
    set(value.freeze())
}
