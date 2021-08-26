package me.sphere.network

import com.apollographql.apollo.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.*
import kotlinx.coroutines.sync.*
import kotlinx.datetime.*
import kotlinx.datetime.Clock
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import me.sphere.appcore.utils.*
import me.sphere.logging.Logger
import okio.ByteString.Companion.encodeUtf8
import kotlin.coroutines.CoroutineContext
import kotlin.time.*

/**
 * Protocol: https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class, ExperimentalTime::class)
internal class GqlWebSocketClient(
    private val httpClient: AgentHTTPClient,
    private val autoreconnect: Duration,
    private val connectionSetupTimeout: Duration = 30.seconds,
    private val logger: Logger
): Closeable, CoroutineScope {
    object OperationCompletedToken: Throwable()

    override val coroutineContext get() = supervisorJob + dispatcher

    private val dispatcher = createSingleThreadDispatcher("GqlWebSocketClient")
    private val supervisorJob = SupervisorJob()

    private val currentConnection = MutableStateFlow<WebSocketConnection?>(null)
    private val lastFailedConnectionAttempt = MutableStateFlow(Instant.DISTANT_PAST)
    private val connectionUpdateMutex = Mutex()

    private val incomingMessages = currentConnection
        .flatMapLatest { connection -> connection?.parsedMessages() ?: emptyFlow() }
        .buffer()
        .shareIn(this, SharingStarted.Eagerly, 0)

    init {
        freeze()
        setupAutoreconnect()
        observeCurrentConnectionClosure()
    }

    override fun close() = supervisorJob.cancel()

    fun <D: Operation.Data, T, V: Operation.Variables> subscribe(
        subscription: Subscription<D, T, V>
    ): Flow<T> = flatMapLatestConnection { connection ->
        val operationId = uuid()
        val request = GqlOutgoingMessage(
            type = GqlOutgoingMessage.Type.Start,
            id = operationId,
            payload = Json.parseToJsonElement(
                subscription.composeRequestBody().utf8()
            ).jsonObject
        )

        incomingMessages
            .onSubscription { connection.send(request) }
            .mapNotNull(request::matchIncoming)
            .conflate()
            .transformWhile { result ->
                when (result) {
                    is GqlEvaluatedIncomingResult.Data -> {
                        val intermediate = Json.encodeToString(JsonElement.serializer(), result.data)
                        val result = subscription.parse(intermediate.encodeUtf8())

                        if (result.hasErrors()) {
                            throw GraphQLHTTPError(result.errors)
                        } else {
                            val data = result.data ?: throw GraphQLHTTPError(null)
                            emit(data)
                        }

                        // Expect more incoming messages. Continue the flow.
                        return@transformWhile true
                    }
                    is GqlEvaluatedIncomingResult.Completed ->
                        // Receive `completed` for this subscription. Close the flow.
                        return@transformWhile false
                    is GqlEvaluatedIncomingResult.Error ->
                        // Error happened along the way for this subscription. Cancel the flow with an exception.
                        throw result.error
                }
            }
            .catch {
                if (it !is GqlWebSocketError) {
                    connection.send(GqlOutgoingMessage(type = GqlOutgoingMessage.Type.Stop, id = operationId))
                    throw it
                }
            }
            .flowOn(dispatcher)
    }

    private fun <R> flatMapLatestConnection(transform: (WebSocketConnection) -> Flow<R>): Flow<R>
        = currentConnection
            .onSubscription { createNewConnectionIfNeeded() }
            .flatMapLatest { connection -> connection?.let(transform) ?: emptyFlow() }

    private suspend fun createNewConnectionIfNeeded() = connectionUpdateMutex.withLock {
        if (currentConnection.value != null) {
            return@withLock
        }

        val connection = httpClient.webSocket(
            HTTPRequest(HTTPRequest.Method.GET, API("/graphql"), null, null, Unit),
            protocol = "graphql-transport-ws"
        )

        val authToken = httpClient.agentAuthToken()
        val params = mapOf("authToken" to authToken)
        val initRequest = GqlOutgoingMessage(
            type = GqlOutgoingMessage.Type.ConnectionInit,
            payload = Json.encodeToJsonElement(
                MapSerializer(String.serializer(), String.serializer()),
                params
            ).jsonObject
        )

        try {
            /**
             * We can't rely on [incomingMessages] here. At this point the connection hasn't received
             * [GqlIncomingMessage.Type.ConnectionAck], and so it isn't assigned to [currentConnection] yet. In other
             * words, the responses won't come through [incomingMessages].
             */
            connection.parsedMessages(onSubscription = { connection.send(initRequest) })
                .map(initRequest::matchIncoming)
                .takeWhile { it != GqlEvaluatedIncomingResult.Completed }
                .timeout(connectionSetupTimeout.toLongMilliseconds()) {
                    TimeoutException("Response for the GraphQL connection_init message has not arrived within the set timeout.")
                }
                .collect()

            currentConnection.value = connection
        } catch (e: Throwable) {
            currentConnection.value = null
            lastFailedConnectionAttempt.value = Clock.System.now()

            logger.exception(e)
        }
    }

    private fun setupAutoreconnect() {
        lastFailedConnectionAttempt
            .mapLatest {
                select<Boolean> {
                    httpClient.isNetworkLikelyAvailable().filter { it }.take(1)
                    onTimeout(autoreconnect) { true }
                }
            }
            .onEach { createNewConnectionIfNeeded() }
            .launchIn(this)
    }

    private fun observeCurrentConnectionClosure() {
        currentConnection
            .flatMapLatest { connection ->
                connection?.let { unwrapped ->
                    unwrapped.status
                        .filter { it is WebSocketConnection.Status.Closed }
                        .take(1)
                        .map { unwrapped }
                } ?: emptyFlow()
            }
            .onEach { closedConnection ->
                connectionUpdateMutex.withLock {
                    if (currentConnection.compareAndSet(closedConnection, null)) {
                        lastFailedConnectionAttempt.value = Clock.System.now()
                    }
                }
            }
            .launchIn(this)
    }
}

private fun WebSocketConnection.parsedMessages(onSubscription: () -> Unit = {}): Flow<Result<GqlIncomingMessage>> = this
    .incomingPayloads
    .onSubscription { onSubscription() }
    .map { payload -> runCatching { Json.decodeFromString(GqlIncomingMessage.serializer(), payload) } }

private fun WebSocketConnection.send(message: GqlOutgoingMessage)
    = send(Json.encodeToString(GqlOutgoingMessage.serializer(), message))
