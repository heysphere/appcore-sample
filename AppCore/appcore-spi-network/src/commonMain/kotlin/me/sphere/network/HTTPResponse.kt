package me.sphere.network

import kotlin.js.JsExport

@JsExport
data class HTTPResponse(
    val statusCode: HTTPStatusCode,
    val body: String
)
