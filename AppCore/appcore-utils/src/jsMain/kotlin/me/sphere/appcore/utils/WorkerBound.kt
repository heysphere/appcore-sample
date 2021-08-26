package me.sphere.appcore.utils


actual class WorkerBound<T: Any> actual constructor(initialValue: () -> T) : Closeable {
    override fun close(): Unit = TODO("Not yet implemented")
    actual fun <R> access(action: T.() -> R): R = TODO("Not yet implemented")
}
