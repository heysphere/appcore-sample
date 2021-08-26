package me.sphere.appcore.utils

import kotlinx.coroutines.*
import platform.CoreFoundation.CFRunLoopGetCurrent
import platform.CoreFoundation.CFRunLoopGetMain
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.native.concurrent.Worker

@OptIn(ExperimentalCoroutinesApi::class)
actual fun createSingleThreadDispatcher(name: String): CoroutineDispatcher = kotlinx.coroutines.newSingleThreadContext(name)
