package me.sphere.appcore.utils

/**
 * A mutable state that is bound to a particular synchronization primitive.
 *
 * Kotlin/Native requires this to be bound to a `Worker` (one specific thread). Note that GCD serial queues do not
 * qualify, since those are executed over a pool of threads.
 *
 * In Kotlin/JVM, this can be relaxed simply as a lock-protected mutable state.
 */
expect class WorkerBound<T: Any>(initialValue: () -> T): Closeable {
    /**
     * Access the mutable state. You can modify `T` in the action lambda.
     *
     * This function traps if `access` is not called on the `EventLoop` it is bound to.
     */
    fun <R> access(action: T.() -> R): R
}
