package me.sphere.test.support

import platform.darwin.*
import kotlin.native.concurrent.freeze

actual class Semaphore actual constructor(count: Int) {
    private val sema = dispatch_semaphore_create(count.toLong())

    init { freeze() }

    actual fun waitForPermit() {
        dispatch_semaphore_wait(sema, DISPATCH_TIME_FOREVER)
    }

    actual fun signalPermit() {
        dispatch_semaphore_signal(sema)
    }
}
