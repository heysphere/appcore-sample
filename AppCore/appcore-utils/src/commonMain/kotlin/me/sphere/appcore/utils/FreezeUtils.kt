package me.sphere.appcore.utils

fun <T, R> frozenLambda(block: (T) -> R): (T) -> R = block.freeze()
fun <T, U, R> frozenLambda(block: (T, U) -> R): (T, U) -> R = block.freeze()
fun <T, U, V, R> frozenLambda(block: (T, U, V) -> R): (T, U, V) -> R = block.freeze()
fun <T, U, V, W, R> frozenLambda(block: (T, U, V, W) -> R): (T, U, V, W) -> R = block.freeze()

fun <T, R> frozenSuspend(block: suspend (T) -> R): suspend (T) -> R = block.freeze()
fun <T, U, R> frozenSuspend(block: suspend (T, U) -> R): suspend (T, U) -> R = block.freeze()
fun <T, U, V, R> frozenSuspend(block: suspend (T, U, V) -> R): suspend (T, U, V) -> R = block.freeze()
fun <T, U, V, W, R> frozenSuspend(block: suspend (T, U, V, W) -> R): suspend (T, U, V, W) -> R = block.freeze()

expect fun <T> T.freeze(): T
