package me.sphere.test.support

import me.sphere.test.Tests
import platform.Foundation.*
import platform.QuartzCore.CACurrentMediaTime
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait
import platform.posix.usleep
import kotlin.native.concurrent.freeze

actual fun Tests.platformBusyWaitImpl(
    timeoutMillis: Int,
    sleepMillis: Int,
    doAction: () -> Unit,
    repeatWhile: () -> Boolean,
    onTimeout: () -> Unit
) {
    require(NSThread.isMainThread)

    val expiry = CACurrentMediaTime() + timeoutMillis.toDouble() / 1000

    do {
        doAction()

        if (repeatWhile()) {
            val wakeUpTime = NSDate.now().dateByAddingTimeInterval(sleepMillis.toDouble() / 1000)
            NSRunLoop.currentRunLoop.runUntilDate(wakeUpTime)
        }
    } while (repeatWhile() && CACurrentMediaTime() <= expiry)

    if (repeatWhile()) {
        onTimeout()
    }
}
