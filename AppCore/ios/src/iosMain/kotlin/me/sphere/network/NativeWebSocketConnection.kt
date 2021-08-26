package me.sphere.network

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import me.sphere.appcore.utils.*
import platform.Foundation.*

private typealias StatusActive = WebSocketConnection.Status.Active
private typealias StatusClosed = WebSocketConnection.Status.Closed

@OptIn(ExperimentalCoroutinesApi::class)
class NativeWebSocketConnection(
    request: NSURLRequest,
    private val originalRequest: HTTPRequest<Unit>
): WebSocketConnection {
    override val incomingPayloads = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 8)
    override val status = MutableStateFlow(StatusActive as WebSocketConnection.Status)

    private val session = NSURLSession.sessionWithConfiguration(
        NSURLSessionConfiguration.ephemeralSessionConfiguration
    )
    private val task = session.webSocketTaskWithRequest(request)

    init {
        freeze()
        task.resume()

        // Register to receive the first message.
        receiveNext()
    }

    override fun send(payload: String): Unit = task.sendMessage(
        NSURLSessionWebSocketMessage(payload),
        frozenLambda { error ->
            if (error != null) {
                processError(error)
            }
        }
    )

    override fun close() = closeLocally(
        CancellationException("the WebSocket is manually closed."),
        NSURLSessionWebSocketCloseCodeNormalClosure
    )

    private fun receiveNext(): Unit = task.receiveMessageWithCompletionHandler(
        frozenLambda { message, error ->
            if (message != null) {
                when (message.type) {
                    NSURLSessionWebSocketMessageTypeString -> {
                        incomingPayloads.tryEmit(message.string!!)

                        // Register to receive the next message.
                        receiveNext()
                    }
                    NSURLSessionWebSocketMessageTypeData ->
                        closeWithUnsupportedFormat()
                }
            } else {
                processError(checkNotNull(error))
            }
        }
    )

    private fun closeWithUnsupportedFormat() = closeLocally(
        WebSocketClosedError(WebSocketCloseCode.UnsupportedData, null),
        NSURLSessionWebSocketCloseCodeUnsupportedData
    )

    private fun closeLocally(error: Throwable, closeCode: Long) {
        val shouldClose = status.compareAndSet(StatusActive, StatusClosed(error))

        if (shouldClose) {
            task.cancelWithCloseCode(closeCode, null)
        }
    }

    private fun processError(error: NSError) {
        val finalError: Throwable = if (
            task.closeCode != NSURLSessionWebSocketCloseCodeInvalid
            && task.closeCode != NSURLSessionWebSocketCloseCodeNormalClosure
        ) {
            WebSocketClosedError(
                WebSocketCloseCode(task.closeCode),
                task.closeReason?.decodeAsUtf8String()
            )
        } else {
            HTTPClientError(checkNotNull(error), originalRequest.toDebugStringRequest())
        }

        status.compareAndSet(StatusActive, StatusClosed(finalError))
    }
}

private fun NSData.decodeAsUtf8String(): String? = NSString.create(this, NSUTF8StringEncoding) as String?
