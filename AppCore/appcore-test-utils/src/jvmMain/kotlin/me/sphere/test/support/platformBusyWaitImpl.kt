package me.sphere.test.support

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import me.sphere.test.Tests
import java.util.concurrent.Semaphore as JavaSemaphore

internal actual fun Tests.platformBusyWaitImpl(
    timeoutMillis: Int,
    sleepMillis: Int,
    doAction: () -> Unit,
    repeatWhile: () -> Boolean,
    onTimeout: () -> Unit
) {
    runBlocking {
        val expiry = System.currentTimeMillis() + timeoutMillis

        do {
            doAction()

            if (repeatWhile()) {
                delay(sleepMillis.toLong())
            }
        } while (repeatWhile() && System.currentTimeMillis() < expiry)

        if (repeatWhile()) {
            onTimeout()
        }
    }
}
