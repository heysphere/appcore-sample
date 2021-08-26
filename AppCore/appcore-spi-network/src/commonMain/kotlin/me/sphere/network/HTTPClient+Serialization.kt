package me.sphere.network

import kotlinx.serialization.*
import kotlinx.serialization.json.Json

suspend fun <RequestBody, ResponseBody> HTTPClient.request(
    request: HTTPRequest<RequestBody>,
    requestSerializationStrategy: SerializationStrategy<RequestBody>,
    responseSerializationStrategy: DeserializationStrategy<ResponseBody>
): ResponseBody
    = request(
        request = request,
        requestMapper = { Json.encodeToString(requestSerializationStrategy, it) },
        responseMapper = { Json.decodeFromString(responseSerializationStrategy, it) }
    )

suspend fun <RequestBody, ResponseBody> HTTPClient.request(
    request: HTTPRequest<RequestBody>,
    requestMapper: (RequestBody) -> String,
    responseMapper: (String) -> ResponseBody
): ResponseBody {

    val rawRequest = request
        .addingHeaders(mapOf("content-type" to "application/json"))
        .mapBody(requestMapper)

    val response = this.request(rawRequest)
    return if (response.statusCode.isSuccessful)
        return responseMapper(response.body)
    else
        throw HTTPServerError(response.statusCode, response.body, rawRequest)
}
