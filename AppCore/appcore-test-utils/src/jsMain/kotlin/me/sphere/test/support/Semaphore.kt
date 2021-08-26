package me.sphere.test.support

actual class Semaphore actual constructor(val count: Int) {
    actual fun waitForPermit(): Unit = TODO()
    actual fun signalPermit(): Unit = TODO()
}
