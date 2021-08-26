package me.sphere.appcore.utils

import kotlinx.atomicfu.*

/**
 * An atomic value. Intended to be used only in multiplatform unit testing.
 *
 * Multiplatform test logic sometimes defines non-field atomic references of varying value type in different test cases,
 * so as to expose captured values back to the test runner thread for assertions. However, AtomicFU JVM rejects any
 * non-field `atomic<V>` declaration. So this box is mainly introduced to keep AtomicFU in JVM builds happy.
 */
class Atomic<V>(initial: V) {
    private val storage = atomic<V>(initial)

    init { freeze() }

    var value: V
        get() = storage.value
        set(newValue) { storage.value = newValue.freeze() }
}
