package me.sphere.test.support

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

actual fun runTesting(body: suspend CoroutineScope.() -> Unit): dynamic
    = GlobalScope.promise { body() }
