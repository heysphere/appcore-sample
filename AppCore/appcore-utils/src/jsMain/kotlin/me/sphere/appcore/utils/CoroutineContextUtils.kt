package me.sphere.appcore.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * JS does not have `newSingleThreadContext`. We stub it simply with a named `CoroutineContext` for now.
 */
actual fun createSingleThreadDispatcher(name: String): CoroutineDispatcher = Dispatchers.Default
