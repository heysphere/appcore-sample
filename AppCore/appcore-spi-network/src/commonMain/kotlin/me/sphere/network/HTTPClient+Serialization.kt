package me.sphere.network

import kotlinx.serialization.*
import kotlinx.serialization.json.Json

suspend fun  HTTPClient.request(
    request: HTTPRequest<Unit>
): Unit = request(
    request,
    requestMapper = {null},
    responseMapper = {}
)

suspend fun <RequestBody : Any, ResponseBody> HTTPClient.request(
    request: HTTPRequest<RequestBody>,
    requestSerializationStrategy: SerializationStrategy<RequestBody>,
    responseSerializationStrategy: DeserializationStrategy<ResponseBody>,
    json: Json = Json,
): ResponseBody
    = request(
        request = request,
        requestMapper = { it?.let { json.encodeToString(requestSerializationStrategy, it) }},
        responseMapper = { json.decodeFromString(responseSerializationStrategy, it) }
    )

suspend fun <RequestBody: Any, ResponseBody> HTTPClient.request(
    request: HTTPRequest<RequestBody>,
    requestMapper: (RequestBody?) -> String?,
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
