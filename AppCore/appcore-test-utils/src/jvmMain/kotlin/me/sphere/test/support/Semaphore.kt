package me.sphere.test.support

import java.util.concurrent.Semaphore

actual class Semaphore actual constructor(val count: Int) {
    private val sema = Semaphore(count, true)

    actual fun waitForPermit() {
        sema.acquire()
    }

    actual fun signalPermit() {
        sema.release()
    }
}
