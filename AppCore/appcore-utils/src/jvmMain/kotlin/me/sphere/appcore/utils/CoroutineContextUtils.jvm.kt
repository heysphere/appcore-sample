package me.sphere.appcore.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
actual fun createSingleThreadDispatcher(name: String): CoroutineDispatcher = kotlinx.coroutines.newSingleThreadContext(name)
