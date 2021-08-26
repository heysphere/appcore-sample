package me.sphere.network

class WebSocketClosedError(val code: WebSocketCloseCode, val reason: String?): Throwable(message = null) {
    override val message: String get() = "Closed (code=${code}) because $reason"
}

data class WebSocketCloseCode(val code: Long) {
    companion object {
        val NormalClosure get() = WebSocketCloseCode(1000)
        val UnsupportedData get() = WebSocketCloseCode(1003)
    }
}
