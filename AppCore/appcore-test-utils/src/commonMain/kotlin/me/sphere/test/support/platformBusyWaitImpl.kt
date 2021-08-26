package me.sphere.test.support

import me.sphere.test.Tests

internal expect fun Tests.platformBusyWaitImpl(
    timeoutMillis: Int = 1000,
    sleepMillis: Int = 100,
    doAction: () -> Unit,
    repeatWhile: () -> Boolean,
    onTimeout: () -> Unit
)
