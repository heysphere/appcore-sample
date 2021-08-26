package me.sphere.network

import kotlinx.serialization.*
import kotlinx.serialization.json.JsonObject
import me.sphere.network.GqlEvaluatedIncomingResult.*

/**
 * Message we (client) send to the GraphQL-over-WebSocket server.
 */
@Serializable
internal data class GqlOutgoingMessage(
    val type: Type,
    val id: String? = null,
    val payload: JsonObject? = null
) {
    @Serializable
    enum class Type {
        @SerialName("connection_init") ConnectionInit,
        @SerialName("start") Start,
        @SerialName("stop") Stop,
        @SerialName("connection_terminate") ConnectionTerminate
    }

    internal fun matchIncoming(result: Result<GqlIncomingMessage>): GqlEvaluatedIncomingResult? = when (type) {
        Type.ConnectionInit -> {
            val response = result.getOrNull()
            when (response?.type) {
                GqlIncomingMessage.Type.ConnectionError ->
                    Error(GqlWebSocketError(response.payload))
                GqlIncomingMessage.Type.ConnectionAck, GqlIncomingMessage.Type.ConnectionKeepAlive ->
                    Completed
                else ->
                    result.exceptionOrNull()?.let(::Error)
            }
        }
        Type.Start -> {
            val response = result.getOrNull()
            when (response?.type) {
                GqlIncomingMessage.Type.Error ->
                    Error(GqlWebSocketError(response.payload))
                GqlIncomingMessage.Type.Data ->
                    Data(checkNotNull(response.payload))
                GqlIncomingMessage.Type.Complete ->
                    Completed
                else ->
                    result.exceptionOrNull()?.let(::Error)
            }
        }
        else -> null
    }
}
