package me.sphere.flowredux

import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take

internal fun assertMainThread() = require(DelegateThreadPolicy.isMainThread()) {
    "Sending actions from background threads is not allowed"
}

fun <Action> emptyEffect() = Effect<Action>(emptyFlow())

fun <Action> fireAndForgetEffect(block: suspend () -> Unit) = Effect<Action>(
    flow { emit(block()) }
        .take(1)
        .flatMapLatest { emptyFlow() }
)
