package me.sphere.network

import kotlinx.serialization.*
import kotlinx.serialization.json.JsonObject

/**
 * Message we (client) receive from the GraphQL-over-WebSocket server.
 */
@Serializable
internal data class GqlIncomingMessage(
    val type: Type,
    val id: String? = null,
    val payload: JsonObject? = null
) {
    @Serializable
    enum class Type {
        @SerialName("connection_error") ConnectionError,
        @SerialName("connection_ack") ConnectionAck,
        @SerialName("data") Data,
        @SerialName("error") Error,
        @SerialName("complete") Complete,
        @SerialName("connection_keep_alive") ConnectionKeepAlive
    }
}
