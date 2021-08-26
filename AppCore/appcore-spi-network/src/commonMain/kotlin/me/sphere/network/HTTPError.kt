package me.sphere.network

abstract class HTTPError(
    val request: HTTPRequest<String>
): RuntimeException() {
    abstract val reason: String
    val errorName = this::class.simpleName ?: "<unknown>"

    override val message: String?
        get() = toString()

    override fun toString(): String = """
        [$errorName] $reason
        Request: ${request.method} ${request.resource}
        Body: ${request.body}
        """.trimIndent()
}
