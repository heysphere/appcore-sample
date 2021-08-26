package me.sphere.sqldelight.operations

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import me.sphere.appcore.utils.*
import me.sphere.logging.Logger
import me.sphere.models.operations.OperationId
import me.sphere.models.operations.OperationStatus
import me.sphere.sqldelight.*

class OperationUtils(
    val database: SqlDatabaseGateway,
    val logger: Logger,
    val StoreScope: StoreScope,
) {
    companion object {
        const val IDLE_TO_STARTED_TIMEOUT: Long = 3000
        const val STARTED_TO_RESULT_TIMEOUT: Long = 30000
    }

    init {
        freeze()
    }

    /**
     * [enqueue] but returns [Unit], so that you can use this in a single-expression function.
     */
    fun <Input, Output> enqueueAndForget(operation: OperationDefinition<Input, Output>, input: Input) {
        enqueue(operation, input)
    }

    /**
     * Enqueue the specified operation with the provided input, and return immediately.
     *
     * If you are interested in the operation output or failure, use `execute()` instead, which provides a
     * `Flow<Output>` to subscribe to.
     */
    fun <Input, Output> enqueue(operation: OperationDefinition<Input, Output>, input: Input): EnqueuingResult<Output> {
        val actor = StoreScope.actorFor(operation)
        val output = actor.beforeEnqueue(input)

        if (output != null) {
            return EnqueuingResult.Completed(output)
        }

        return upsert(operation, input).let { EnqueuingResult.Enqueued(it) }
    }

    /**
     * Listens for changes in the [ManagedOperation.status] for a specific [operation] with [input].
     *
     * Note that only operations with an [DeduplicatingInput] input payload can be reflected.
     */
    fun <Input: DeduplicatingInput, Output> listenStatusOf(operation: OperationDefinition<Input, Output>, input: Input) = flow {
        val uniqueKey = input.deduplicationKey
        val id = database.managedOperationQueries
            .operationIdForUniqueKey(StoreScope.clientType, operationType = operation.identifier, uniqueKey = uniqueKey)
            .listenOneOrNull()
            .filterNotNull()
            .first()

        emitAll(
            database.managedOperationQueries.getOperationById(id)
                .listenOne()
                .map { it.status }
        )
    }

    /**
     * Listen for the output or failure from an already enqueued operation.
     *
     * Use `execute()` if you need to enqueue an operation _and_ immediately start listening to it afterwards.
     */
    fun <Input, Output> listen(operation: OperationDefinition<Input, Output>, id: OperationId): Flow<Output> {
        return listenForOperation(id = id.rawValue)
            .filter { it.status == OperationStatus.Success || it.status == OperationStatus.Failure || it.status == OperationStatus.Suspended }
            .take(1)
            .timeout(STARTED_TO_RESULT_TIMEOUT) {
                TimeoutException("${operation.identifier} (id=${id}) has not completed within the ${STARTED_TO_RESULT_TIMEOUT}ms time limit.")
            }
            .map { managedOperation ->
                when (managedOperation.status) {
                    OperationStatus.Success ->
                        managedOperation.output?.let { Json.decodeFromString(operation.outputSerializer, it) }
                            ?: throw OperationUnexpectedStateException(
                                operation.identifier,
                                "Operation is successful, but has no output available for deserialization."
                            )
                    OperationStatus.Failure -> {
                        val digest = managedOperation.output?.let { Json.decodeFromString(OperationErrorDigest.serializer(), it) }
                            ?: throw OperationUnexpectedStateException(
                                operation.identifier,
                                "Operation has failed, but no error digest is available."
                            )
                        throw OperationFailedException(operation.identifier, id.rawValue, digest.code, digest.message)
                    }
                    OperationStatus.Suspended ->
                        throw OperationSuspendedException(operation.identifier, id.rawValue)
                    OperationStatus.Idle, OperationStatus.Started -> throw OperationUnexpectedStateException(
                        operation.identifier,
                        "Invalid status: ${managedOperation.status}."
                    )
                }
            }
    }

    /**
     * Return a `Flow` which enqueues the specified operation with the provided input, and emits the final operation
     * output.
     *
     * If you just want to fire and forget, use `enqueue()`.
     */
    @OptIn(FlowPreview::class)
    @Suppress("UNCHECKED_CAST")
    fun <Input, Output> execute(operation: OperationDefinitionBase<Input, Output>, input: Input): Flow<Output> {
        return when (operation) {
            is SubscribeOperationDefinition<Input, Output> ->
                StoreScope.actorFor(operation).perform(input)
                    .onStart { logger.info { "[sub.${operation::class.simpleName}] Subscribed with input: $input" } }
                    .onCompletion {
                        if (it != null && it !is CancellationException)
                            logger.info { "[sub.${operation::class.simpleName}] Failed for input $input with error: $it" }
                        else
                            logger.info { "[sub.${operation::class.simpleName}] Closed for input $input" }
                    }
            is OperationDefinition<Input, Output> ->
                executeSerializable(operation, input)
            else -> throw IllegalArgumentException("Unrecognized type of operation definition.")
        }
    }

    private fun <Input, Output> executeSerializable(
        operation: OperationDefinition<Input, Output>,
        input: Input
    ): Flow<Output> = flow {
        when (val result = enqueue(operation, input)) {
            is EnqueuingResult.Completed ->
                emit(result.output)

            is EnqueuingResult.Enqueued -> emitAll(
                detectOperationStart(result.id, operation.identifier)
                    .flatMapOnce { listen(operation, result.id) }
                    .flowOn(StoreScope.ActorListen)
            )
        }
    }

    private fun detectOperationStart(id: OperationId, identifier: String): Flow<Unit> = this
        .listenForOperation(id = id.rawValue)
        .filter { it.status != OperationStatus.Idle }
        .take(1)
        .timeout(IDLE_TO_STARTED_TIMEOUT) {
            TimeoutException("$identifier (id=${id}) has not been picked up within the ${IDLE_TO_STARTED_TIMEOUT}ms time limit.")
        }
        .map {}

    private fun listenForOperation(id: Long): Flow<ManagedOperation> =
        database.managedOperationQueries.getOperationById(id).listenOne()

    private fun <Input, Output> upsert(
        operation: OperationDefinition<Input, Output>,
        input: Input
    ) = database.transactionWithResult<OperationId> {
        // If deduplication has not been requested by the type, generate a UUID for the enqueue.
        val uniqueKey = (input as? DeduplicatingInput)?.deduplicationKey ?: uuid()

        database.managedOperationQueries.enqueue(
            clientType = StoreScope.clientType,
            operationType = operation.identifier,
            uniqueKey = uniqueKey,
            input = Json.encodeToString(operation.inputSerializer, input),
            lastUpdated = Clock.System.now()
        )

        database.managedOperationQueries
            .operationIdForUniqueKey(StoreScope.clientType, operation.identifier, uniqueKey)
            .executeAsOne()
            .let(::OperationId)
    }
}
