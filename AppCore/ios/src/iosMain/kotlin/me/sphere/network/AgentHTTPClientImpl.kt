package me.sphere.network

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import me.sphere.appcore.*
import me.sphere.appcore.utils.*
import platform.Foundation.*
import platform.Network.*
import platform.darwin.dispatch_get_main_queue
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class AgentHTTPClientImpl(defaultHeaders: Map<Any?, Any>, delegate: NSURLSessionDelegateProtocol?): AgentHTTPClient {
    private val session: NSURLSession
    private val environment = MutableStateFlow<AppEnvironment?>(null)
    private val authToken = MutableStateFlow<String?>(null)

    init {
        val configuration = NSURLSessionConfiguration.ephemeralSessionConfiguration
        configuration.HTTPAdditionalHeaders = defaultHeaders
        configuration.HTTPCookieStorage = null
        
        val queue = NSOperationQueue()
        queue.qualityOfService = NSQualityOfServiceUtility

        session = NSURLSession.sessionWithConfiguration(configuration, delegate, queue)

        freeze()
    }

    override suspend fun agentAuthToken(): String
        = authToken.first() ?: throw NoCredentialError()

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun isNetworkLikelyAvailable(): Flow<Boolean>
        = channelFlow {
            try {
                val monitor = nw_path_monitor_create()
                nw_path_monitor_set_update_handler(
                    monitor,
                    frozenLambda { path ->
                        try { offer(nw_path_get_status(path) == nw_path_status_satisfied) }
                        catch (e: Throwable) {}
                    }
                )
                nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
                nw_path_monitor_start(monitor)
                awaitClose { nw_path_monitor_cancel(monitor) }
            } catch (e: Throwable) {
                close(e)
            }
        }
        .distinctUntilChanged()

    override suspend fun webSocket(request: HTTPRequest<Unit>, protocol: String): WebSocketConnection {
        check(request.method == HTTPRequest.Method.GET) {
            "WebSocket request must be `GET`."
        }

        val environment = environment.first() ?: throw NoCredentialError()
        val cocoaRequest = makeWebSocketRequest(request, protocol, environment)
        return NativeWebSocketConnection(cocoaRequest, request)
    }

    override suspend fun request(request: HTTPRequest<String>): HTTPResponse {
        return authToken
            .combine(environment) { token, environment ->
                if (token == null || environment == null) {
                    throw HTTPServerError.Unauthorized(request)
                }

                makeURLRequest(request, token, environment)
            }
            .flatMapOnce { nativeRequest ->
                flowForRequestViaURLSession(nativeRequest, request)
            }.first()
    }

    fun setAuthToken(authToken: String?) {
        this.authToken.value = authToken
    }

    fun setEnvironment(environment: AppEnvironment?) {
        this.environment.value = environment
    }

    @Suppress("UNCHECKED_CAST")
    private fun makeWebSocketRequest(request: HTTPRequest<Unit>, protocol: String, environment: AppEnvironment): NSURLRequest = autoreleasepool {
        val url = when(request.resource) {
            is Absolute -> makeURL((request.resource as Absolute).url, null, request.urlQuery, request)
            is API -> makeURL(environment.apiBaseUrl, (request.resource as API).path, request.urlQuery, request)
        }

        check(url.scheme == "wss") {
            "Environment base url `${url}` does not point to a WebSocket Secure (wss://) endpoint."
        }

        val urlRequest = NSMutableURLRequest(url)
        val allHeaders = (request.headers ?: emptyMap()) + ("Sec-WebSocket-Protocol" to protocol)
        urlRequest.setAllHTTPHeaderFields(allHeaders as Map<Any?, *>)
        urlRequest.setHTTPMethod("GET")
        urlRequest.setHTTPBody(null)

        return urlRequest
    }

    @Suppress("UNCHECKED_CAST")
    private fun makeURLRequest(request: HTTPRequest<String>, token: String, environment: AppEnvironment): NSURLRequest = autoreleasepool {
        val url = when(request.resource) {
            is Absolute -> makeURL((request.resource as Absolute).url, null, request.urlQuery, request)
            is API -> makeURL(environment.apiBaseUrl, (request.resource as API).path, request.urlQuery, request)
        }

        val urlRequest = NSMutableURLRequest(url)
        urlRequest.setHTTPMethod(request.method.name)

        val allHeaders = (request.headers ?: emptyMap()) + mapOf("authorization" to "Bearer $token")
        urlRequest.setAllHTTPHeaderFields(allHeaders as Map<Any?, *>)

        when (request.method) {
            HTTPRequest.Method.POST -> {
                @Suppress("CAST_NEVER_SUCCEEDS")
                urlRequest.setHTTPBody((request.body as NSString).dataUsingEncoding(NSUTF8StringEncoding))
            }
            HTTPRequest.Method.GET -> urlRequest.setHTTPBody(null)
        }

        return urlRequest
    }

    @Suppress("UNCHECKED_CAST")
    private fun makeURL(baseUrl: String, path: String? = null, queries: Map<String, String>?, request: HTTPRequest<*>): NSURL {
        val components = NSURLComponents.componentsWithString(baseUrl)
            ?: throw HTTPClientError.Other("Invalid base URL: `${baseUrl}`", request.toDebugStringRequest())

        if (path != null) {
            check(path.startsWith("/"))

            val existingPath = (components.path ?: "").removeSuffix("/")
            components.path = existingPath + path
        }

        val queryItems = components.queryItems?.toMutableList() ?: mutableListOf()
        queryItems.addAll(
            (queries ?: emptyMap()).map { NSURLQueryItem.queryItemWithName(it.key, it.value) }
        )
        components.setQueryItems(queryItems)

        return components.URL
            ?: throw HTTPClientError.Other(
                "Failed to construct URL for base URL `${baseUrl}` and path `${path ?: "<null>"}`.",
                request.toDebugStringRequest()
            )
    }

    private fun flowForRequestViaURLSession(nativeRequest: NSURLRequest, request: HTTPRequest<String>): Flow<HTTPResponse> = callbackFlow {
        val task = autoreleasepool {
            session.dataTaskWithRequest(
                nativeRequest,
                frozenLambda { data, response, error ->
                    if (response == null) {
                        val kotlinError = error?.let { HTTPClientError(it, request) }
                            ?: IllegalStateException("Got no response and also no error.")

                        close(kotlinError)
                        return@frozenLambda
                    }

                    if (response !is NSHTTPURLResponse) {
                        close(IllegalStateException("Response class is not NSHTTPURLResponse."))
                        return@frozenLambda
                    }

                    val body = data?.let { NSString.create(it, NSUTF8StringEncoding) as String? } ?: ""
                    val statusCode = HTTPStatusCode(response.statusCode.toInt())

                    if (response.statusCode in 200..299) {
                        val finalResponse = HTTPResponse(statusCode, body)

                        try {
                            offer(finalResponse.freeze())
                            close()
                        } catch (e: Throwable) {}
                    } else {
                        val kotlinError = HTTPServerError(statusCode, body, request)
                        close(kotlinError.freeze())
                    }
                }
            )
        }

        task.resume()
        awaitClose(task::cancel)
    }
}

internal fun HTTPClientError(error: NSError, request: HTTPRequest<String>): HTTPClientError {
    if (error.domain == NSURLErrorDomain) {
        when (error.code) {
            NSURLErrorCannotConnectToHost, NSURLErrorCannotFindHost,
            NSURLErrorDNSLookupFailed, NSURLErrorSecureConnectionFailed
                -> return HTTPClientError.Unreachable(request)
            NSURLErrorNotConnectedToInternet, NSURLErrorDataNotAllowed, NSURLErrorInternationalRoamingOff,
            NSURLErrorCallIsActive, NSURLErrorNetworkConnectionLost
                -> return HTTPClientError.Offline(request)
            NSURLErrorTimedOut
                -> return HTTPClientError.Timeout(request)
        }
    }

    return HTTPClientError.Other("${error.domain} ${error.code} ${error.localizedDescription}", request)
}

internal fun HTTPRequest<*>.toDebugStringRequest(): HTTPRequest<String> = mapBody {
    when (it) {
        is String -> it
        is Unit -> ""
        else -> it.toString()
    }
}
