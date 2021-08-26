package me.sphere.network

import kotlinx.coroutines.flow.Flow

interface HTTPClient: ConnectivityMonitor {
    suspend fun request(request: HTTPRequest<String>): HTTPResponse

    suspend fun webSocket(request: HTTPRequest<Unit>, protocol: String): WebSocketConnection
}
