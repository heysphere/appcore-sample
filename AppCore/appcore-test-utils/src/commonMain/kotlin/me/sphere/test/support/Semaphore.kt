package me.sphere.test.support

expect class Semaphore(count: Int) {
    fun waitForPermit()
    fun signalPermit()
}
