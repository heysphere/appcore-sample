package me.sphere.network

class HTTPServerError(
    val statusCode: HTTPStatusCode,
    val body: String? = null,
    request: HTTPRequest<String>
): HTTPError(request) {
    override val reason: String
        get() = "Code $statusCode"

    companion object {
         fun Unauthorized(request: HTTPRequest<String>)
            = HTTPServerError(HTTPStatusCode.Unauthorized, null, request)
    }
}
