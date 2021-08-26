package me.sphere.network

import kotlinx.coroutines.flow.*

interface WebSocketConnection {
    val incomingPayloads: SharedFlow<String>
    val status: StateFlow<Status>

    fun send(payload: String)
    fun close()

    sealed class Status {
        object Active: Status()
        class Closed(reason: Throwable): Status()
    }
}
