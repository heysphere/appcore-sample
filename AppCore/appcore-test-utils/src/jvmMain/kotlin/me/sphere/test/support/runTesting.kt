package me.sphere.test.support

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

actual fun runTesting(body: suspend CoroutineScope.() -> Unit) = runBlocking { body() }
