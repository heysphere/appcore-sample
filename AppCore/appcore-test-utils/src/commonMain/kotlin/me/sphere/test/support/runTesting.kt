package me.sphere.test.support

import kotlinx.coroutines.CoroutineScope

expect fun runTesting(body: suspend CoroutineScope.() -> Unit)
