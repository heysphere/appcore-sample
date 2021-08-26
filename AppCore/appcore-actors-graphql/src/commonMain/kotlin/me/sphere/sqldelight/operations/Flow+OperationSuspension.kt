package me.sphere.sqldelight.operations

import me.sphere.network.HTTPClientError

suspend fun <T> suspendOnConnectivityError(
    action: suspend () -> T,
    onSuspension: suspend () -> Unit = {},
    onFailure: suspend () -> Unit = {}
) {
    try {
        action()
    } catch (e: Exception) {
        when {
            e.isConnectivityError -> {
                onSuspension()
                throw NeedsSuspensionException()
            }
            else -> {
                onFailure()
                throw e
            }
        }
    }
}

val Throwable.isConnectivityError: Boolean
    get() = this is HTTPClientError.Unreachable || this is HTTPClientError.Offline || this is HTTPClientError.Timeout
