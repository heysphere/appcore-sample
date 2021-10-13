package me.spjere.appcore.android.network

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.suspendCancellableCoroutine
import me.sphere.network.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.EMPTY_REQUEST
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException
import java.security.cert.CertificateException
import javax.net.ssl.SSLHandshakeException
import kotlin.coroutines.resumeWithException

class HTTPClientImpl(
    private val okHttpClient: OkHttpClient,
    private val networkObserver: NetworkObserver,
    host: String
) : HTTPClient {

    override suspend fun request(request: HTTPRequest<String>): HTTPResponse {
        val okHttpRequest = buildOkHttpRequest(request)
        val call = okHttpClient.newCall(okHttpRequest)
        return suspendCancellableCoroutine { continuation ->
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    val exception = if (e.isRecoverable()) {
                        HTTPClientError.Offline(request)
                    } else {
                        HTTPClientError.Other(e.localizedMessage ?: "API Error", request)
                    }

                    continuation.resumeWithException(exception)
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.byteString()?.utf8() ?: ""
                    if (response.code in 200..299) {
                        continuation.resume(
                            HTTPResponse(HTTPStatusCode(response.code), body)
                        ) {
                            // No op, request is finished
                        }
                    } else {
                        val errorMessage = "body: $body, errorMessage: ${response.message}"
                        val statusCode = HTTPStatusCode(response.code)
                        val error = HTTPServerError(statusCode, errorMessage, request)
                        continuation.resumeWithException(error)
                    }
                }
            })
            continuation.invokeOnCancellation {
                call.cancel()
            }
        }
    }

    override suspend fun webSocket(
        request: HTTPRequest<Unit>,
        protocol: String
    ): WebSocketConnection {
        TODO("Web sockets are not used in the example")
    }

    private fun buildOkHttpRequest(request: HTTPRequest<String>): Request {
        val httpUrl = buildUrl(request.resource, request.urlQuery)
        val mediaType = (request.headers?.get("content-type") ?: "application/json").toMediaType()
        val httpRequestBody = request.body
        val requestBuilder = Request.Builder()
            .url(httpUrl)
            .method(
                request.method.name,
                when (request.method) {
                    HTTPRequest.Method.GET,
                    HTTPRequest.Method.POST -> httpRequestBody?.toRequestBody(mediaType)
                    HTTPRequest.Method.PATCH -> httpRequestBody?.toRequestBody(mediaType)
                        ?: EMPTY_REQUEST
                }
            )
        request.headers?.forEach { header ->
            requestBuilder.addHeader(header.key, header.value)
        }
        return requestBuilder.build()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun isNetworkLikelyAvailable(): Flow<Boolean> {
        return channelFlow {
            val networkListener = NetworkObserver.Listener { isOnline ->
                trySend(isOnline)
            }
            networkObserver.addListener(networkListener)

            awaitClose {
                networkObserver.removeListener(networkListener)
            }
        }.distinctUntilChanged()
    }

    private fun buildUrl(resource: HTTPResource, urlQuery: Map<String, String>?): HttpUrl {
        val httpUrlBuilder = HttpUrl.Builder()
            .scheme("https")
        when (resource) {
            is Absolute -> httpUrlBuilder.host(resource.url)
            is API -> {
                httpUrlBuilder.host(apiHost)
                httpUrlBuilder.addPathSegments(removeSlashPrefixFromPath(resource.path))
            }
        }
        urlQuery?.forEach { query ->
            httpUrlBuilder.addQueryParameter(query.key, query.value)
        }
        return httpUrlBuilder.build()
    }

    private val apiHost = host.subSequence(8, host.length).toString()
    private fun removeSlashPrefixFromPath(path: String) =
        if (path[0] == '/') {
            path.subSequence(1, path.length).toString()
        } else {
            path
        }

    private fun IOException.isRecoverable(): Boolean = when (this) {
        is SSLHandshakeException -> cause !is CertificateException
        is UnknownHostException -> true
        is ConnectException -> true
        else -> false
    }
}
