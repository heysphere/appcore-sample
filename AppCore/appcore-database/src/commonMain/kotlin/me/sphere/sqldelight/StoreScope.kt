package me.sphere.sqldelight

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import me.sphere.appcore.utils.*
import me.sphere.logging.Logger
import me.sphere.models.BackendEnvironmentType
import me.sphere.sqldelight.operations.OperationDefinition
import me.sphere.sqldelight.operations.OperationStoreActorBase
import me.sphere.sqldelight.operations.SubscribeOperationActor
import me.sphere.sqldelight.operations.SubscribeOperationDefinition

class StoreScope(
    val gitHubUserName: String,
    val gitHubAccessToken: String,
    val clientType: StoreClientType,
    val environmentType: BackendEnvironmentType,
    operationQueries: ManagedOperationQueries,
    logger: Logger,
    overrideDispatcher: CoroutineDispatcher? = null
): Closeable {
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        logger.exception(throwable)
    }

    private val supervisorJob = SupervisorJob()
    private val subscribeActorRegistry = Atomic<Map<SubscribeOperationDefinition<*, *>, SubscribeOperationActor<*, *>>>(emptyMap())
    private val operationActorRegistry = Atomic<Map<OperationDefinition<*, *>, OperationStoreActorBase<*, *>>>(emptyMap())

    /**
     * Coroutine Context suitable for any work writing the SQLCore database.
     */
    val Write = (overrideDispatcher ?: createSingleThreadDispatcher("SphereStoreScope.Write")) + exceptionHandler

    /**
     * Coroutine Context suitable for listening the SQLCore database.
     */
    val ActorListen = (overrideDispatcher ?: createSingleThreadDispatcher("SphereStoreScope.ActorListen")) + exceptionHandler

    /**
     * A Coroutine Scope suitable for listening to the SQLCore database.
     */
    val ActorListenScope = CoroutineScope(supervisorJob + ActorListen)

    /**
     * A Coroutine Scope suitable for work writing the SQLCore database that does not have an explicit Store Actor owner.
     */
    val WriteScope = CoroutineScope(supervisorJob + Write)

    /**
     * A Coroutine Scope suitable for listening on the main thread.
     */
    val MainScope = CoroutineScope(supervisorJob + Dispatchers.Main)

    /**
     * Share one database listener across all operation actors for idle operations awaiting to be picked up.
     */
    private val operationsAwaitingActorPickup: SharedFlow<List<GetOperationsAwaitingActorPickup>> = operationQueries
        .getOperationsAwaitingActorPickup(clientType)
        .listenAll(ListenerOption.NoThrottle)
        // buffer = 0 suspends the emitter if ShareFlow subscriber call-out is slower. This creates backpressure
        // towards the upstream `listenAll()`, which can in turn adapt by slowing down the database querying.
        //
        // Note that there is no backpressure here from the execution of the subscribers, since all subscribers are
        // compulsorily `conflate()` below.
        .buffer(0)
        .shareIn(ActorListenScope, SharingStarted.WhileSubscribed(), replay = 1)

    fun register(actors: Iterable<*>) {
        val subscribeActors = actors.mapNotNull { it as? SubscribeOperationActor<*, *> }
        subscribeActorRegistry.value = subscribeActors.associateBy { it.definition }.freeze()

        val operationActors = actors.mapNotNull { it as? OperationStoreActorBase<*, *> }
        operationActorRegistry.value = operationActors.associateBy { it.definition }.freeze()
    }

    @Suppress("UNCHECKED_CAST")
    fun <Input, Output> actorFor(definition: SubscribeOperationDefinition<Input, Output>): SubscribeOperationActor<Input, Output>
        = subscribeActorRegistry.value[definition] as? SubscribeOperationActor<Input, Output>
            ?: throw IllegalArgumentException("Unregistered definition, for $definition.")

    @Suppress("UNCHECKED_CAST")
    fun <Input, Output> actorFor(definition: OperationDefinition<Input, Output>): OperationStoreActorBase<Input, Output>
        = operationActorRegistry.value[definition] as? OperationStoreActorBase<Input, Output>
            ?: throw IllegalArgumentException("Unregistered definition, for $definition.")

    fun <Input, Output> listenNewEnqueueIds(definition: OperationDefinition<Input, Output>) = definition.identifier.let { identifier ->
        operationsAwaitingActorPickup
            .map { candidates -> candidates.filter { it.operationType == identifier } }
            // Conflate the filtered output. This guarantees that slow downstream execution can never stall the upstream
            // ShareFlow, while the downstream still gets notified of the latest emission at their own pace.
            //
            // Must be applied BEFORE `transformInsertions`
            .conflate()
            .transformInsertions { emit(it.id) }
    }

    override fun close() = supervisorJob.cancel()
    suspend fun closeAndJoin() = supervisorJob.cancelAndJoin()
}
