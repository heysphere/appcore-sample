package me.sphere.test

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import me.sphere.appcore.utils.Atomic
import me.sphere.appcore.utils.freeze
import me.sphere.appcore.utils.frozenLambda
import me.sphere.network.HTTPClient
import me.sphere.network.HTTPRequest
import me.sphere.network.HTTPResponse
import me.sphere.network.WebSocketConnection
import kotlin.test.fail

class StubHTTPClient: HTTPClient {
    val stubRequest = Atomic<(HTTPRequest<String>) -> Flow<HTTPResponse>>(
        frozenLambda { _ -> fail("Unexpected call") }
    )

    init { freeze() }

    override fun isNetworkLikelyAvailable(): Flow<Boolean> = emptyFlow()
    override suspend fun request(request: HTTPRequest<String>)
        = stubRequest.value.invoke(request).first()

    override suspend fun webSocket(request: HTTPRequest<Unit>, protocol: String): WebSocketConnection {
        TODO("Not yet implemented")
    }
}
