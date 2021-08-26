package me.sphere.appcore.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.*

expect fun createSingleThreadDispatcher(name: String): CoroutineDispatcher

fun <T> Deferred<T>.asFlow(): Flow<T> = flow { emit(await()) }

@OptIn(ExperimentalCoroutinesApi::class)
fun <T, U> Flow<T>.flatMapOnce(mapper: suspend (T) -> Flow<U>): Flow<U> = take(1).flatMapLatest(mapper)
