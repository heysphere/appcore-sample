package me.sphere.test.support

import me.sphere.test.Tests

internal actual fun Tests.platformBusyWaitImpl(
    timeoutMillis: Int,
    sleepMillis: Int,
    doAction: () -> Unit,
    repeatWhile: () -> Boolean,
    onTimeout: () -> Unit
) {
    TODO()
}
