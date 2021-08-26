package me.sphere.network

import kotlinx.serialization.json.JsonObject

internal data class GqlWebSocketError(
    val payload: JsonObject?
): Throwable()
