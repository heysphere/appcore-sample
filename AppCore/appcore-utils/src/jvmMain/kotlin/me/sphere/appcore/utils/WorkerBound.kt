package me.sphere.appcore.utils

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

actual class WorkerBound<T: Any> actual constructor(initialValue: () -> T) : Closeable {
    private val disposed = AtomicBoolean(false)
    private val lock = ReentrantLock()
    private var value: T = initialValue()

    override fun close() {
        disposed.compareAndSet(false, true)
    }

    actual fun <R> access(action: T.() -> R): R = lock.withLock { action(value) }
}
