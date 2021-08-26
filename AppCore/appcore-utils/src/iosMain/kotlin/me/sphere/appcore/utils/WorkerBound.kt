package me.sphere.appcore.utils

import kotlinx.atomicfu.atomic
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.WorkerBoundReference

actual class WorkerBound<T: Any> actual constructor(private val initialValue: () -> T): Closeable {
    private val refContainer = AtomicReference<WorkerBoundReference<T>?>(null)
    private val disposed = atomic(false)

    init { freeze() }

    actual fun <R> access(action: T.() -> R): R {
        check(!disposed.value)

        val workerBoundRef = refContainer.value ?: {
            val workerBoundRef = WorkerBoundReference(initialValue()).freeze()
            val isSet = refContainer.compareAndSet(null, workerBoundRef)
            check(isSet, ::illegalConcurrentAccess)
            workerBoundRef
        }()

        check(workerBoundRef.worker == Worker.current, ::illegalConcurrentAccess)
        return action(workerBoundRef.value)
    }

    override fun close() {
        if (disposed.compareAndSet(expect = false, update = true)) {
            val workerBoundRef = refContainer.value
            val isCleared = refContainer.compareAndSet(workerBoundRef, null)
            check(isCleared, ::illegalConcurrentAccess)
        }
    }
}

private fun illegalConcurrentAccess(): String
    = "Detected illegal concurrent access to `WorkerBound`."
