package me.sphere.network

import kotlin.js.JsExport

@JsExport
data class HTTPStatusCode(
    val rawValue: Int
) {
    val isSuccessful: Boolean
        get() = (200..299).contains(rawValue)

    companion object {
        val Unauthorized
            get() = HTTPStatusCode(401)
    }
}
