package me.sphere.network

sealed class HTTPClientError(request: HTTPRequest<String>): HTTPError(request) {
    class Unreachable(request: HTTPRequest<String>): HTTPClientError(request) {
        override val reason get() = "Network is unreachable"
    }
    class Offline(request: HTTPRequest<String>) : HTTPClientError(request) {
        override val reason get() = "Network is offline"
    }
    class Timeout(request: HTTPRequest<String>) : HTTPClientError(request) {
        override val reason get() = "Request is timed out"
    }
    class Other(override val reason: String, request: HTTPRequest<String>): HTTPClientError(request)
}

