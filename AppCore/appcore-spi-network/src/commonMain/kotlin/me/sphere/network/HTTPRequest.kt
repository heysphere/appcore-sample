package me.sphere.network

data class HTTPRequest<Body : Any>(
    val method: Method,
    val resource: HTTPResource,
    val urlQuery: Map<String, String>? = null,
    val headers: Map<String, String>? = null,
    val body: Body?
) {
    fun <NewBody : Any> mapBody(transform: (Body?) -> NewBody?)
        = HTTPRequest(method, resource, urlQuery, headers, body.let(transform))

    fun addingHeaders(newHeaders: Map<String, String>)
        = this.copy(headers = (headers ?: emptyMap()) + newHeaders)

    fun addingQuery(newQueries: Map<String, String>)
        = this.copy(urlQuery = (urlQuery ?: emptyMap()) + newQueries)

    enum class Method {
        GET, POST, PATCH
    }
}
