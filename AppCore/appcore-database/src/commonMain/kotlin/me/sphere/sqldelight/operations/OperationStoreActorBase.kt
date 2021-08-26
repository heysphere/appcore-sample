package me.sphere.sqldelight.operations

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import me.sphere.appcore.utils.*
import me.sphere.logging.Logger
import me.sphere.sqldelight.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class, ExperimentalTime::class)
abstract class OperationStoreActorBase<Input, Output>(
    val database: SqlDatabaseGateway,
    val logger: Logger,
    StoreScope: StoreScope,
    /**
     * This is an ultimate last-resort timeout. The expectation is that Firestore and platform networking should have a
     * way more aggressive request timeout (typically in the range of 15-30 seconds).
     */
    private val executionTimeout: Duration = 60.seconds
): StoreActorBase(StoreScope) {
    abstract val definition: OperationDefinition<Input, Output>
    private val loggingPrefix get() = "[actor.${definition.identifier}] "

    /**
     * Fast path of your operation.
     *
     * If the actor can potentially provide a fast synchronous output for the operation, override this method to
     * implement such fast path. The entirety of the operation lifecycle is skipped for the fast path output, avoiding
     * overhead in asynchronous dispatch and task management.
     *
     * The actor may also use [beforeEnqueue] to perform early validation of the [Input] parameters.
     *
     * For example, given a `SpanishWordToPortugeseWordOperation`, you may have:
     * - a [beforeEnqueue] fast path, returning a cached translation in the database, or `null` if the lookup is missed.
     * - a [perform] slow path, performing a more expensive API request to obtain the translation and storing it in the
     *   database.
     *
     * @return The output to be emitted synchronously, or `null` if the output is indeterminate.
     */
    open fun beforeEnqueue(input: Input): Output? = null

    /**
     * Perform the operation given the input.
     *
     * If `NeedsSuspensionException` is thrown by either the `perform()` body or the created `Flow`, SQLCore
     * parks the operation in an `AwaitingNetwork` state. All subscribers listening via `execute()` fails with
     * `OperationSuspendedException`, but the operation itself is not considered failed.
     *
     * A suspended operation will eventually be re-executed when either:
     *
     * - An operation with the same uniqueing signature is enqueued (where the new input payload will override the
     *   stored input payload); or
     * - The host reports that network connectivity has resumed.
     */
   abstract suspend fun perform(input: Input): Output

    final override fun attach() {
        val queries = database.managedOperationQueries

        StoreScope.listenNewEnqueueIds(definition)
            .flatMapMerge { operationId ->
                logger.info { "$loggingPrefix Recognized id=$operationId" }

                val decodeAndApplyInput = flow {
                    val operation = queries.getOperationById(operationId).executeAsOneOrNull()
                        ?: throw RuntimeException("Operation $operationId should exist but query returns null.")

                    val input = Json.decodeFromString(this@OperationStoreActorBase.definition.inputSerializer, operation.input)
                    queries.actorSetStarted(Clock.System.now(), operationId)

                    logger.info { "$loggingPrefix Started id=$operationId key=${(input as? DeduplicatingInput)?.deduplicationKey}" }

                    emit(perform(input))
                }

                return@flatMapMerge decodeAndApplyInput
                    .onEach {
                        val output = Json.encodeToString(definition.outputSerializer, it)
                        queries.actorSetSuccess(Clock.System.now(), output, operationId)
                        logger.info { "$loggingPrefix Success id=$operationId" }
                    }
                    .timeout(executionTimeout.toLongMilliseconds()) {
                        logger.info { "$loggingPrefix Timeout id=$operationId" }
                        TimeoutException("$actorName did not finish execution within the configured timeout: $executionTimeout.")
                    }
                    .onEmpty {
                        logger.info { "$loggingPrefix Empty id=$operationId" }
                        throw IllegalStateException("`perform()` in $actorName has submitted work that completes without throwing any error or output.")
                    }
                    .catch {
                        when (it) {
                            is NeedsSuspensionException -> {
                                queries.actorSetSuspended(Clock.System.now(), operationId)
                                logger.info { "$loggingPrefix Suspend id=$operationId" }
                            }
                            else -> {
                                val errorDigest = OperationErrorDigest(
                                    code = OperationErrorCode.DEFAULT,
                                    message = (it as? HumanReadableException)?.messageForHuman
                                ).let { Json.encodeToString(OperationErrorDigest.serializer(), it) }
                                queries.actorSetFailure(Clock.System.now(), errorDigest = errorDigest, id = operationId)
                                logger.info { "$loggingPrefix Failed id=$operationId" }
                                logger.exception(it)
                            }
                        }
                    }
                    .map {}
            }
            .launchIn(StoreScope.ActorListenScope)
    }
}
