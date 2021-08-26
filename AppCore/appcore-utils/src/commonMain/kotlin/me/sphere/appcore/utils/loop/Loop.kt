package me.sphere.appcore.utils.loop

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.*
import me.sphere.appcore.utils.*
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalStdlibApi::class, FlowPreview::class)
class Loop<State, Event>(
    private val parent: CoroutineScope,
    initial: State,
    private val reducer: LoopReducer<State, Event>,
    action: LoopBuilder<State, Event>.() -> Unit
) {
    val state: Flow<State>
        get() = flow.takeWhile { it.isActive }.map { it.state }

    private val flow = MutableSharedFlow<LoopSnapshot<State, Event>>(replay = 1, extraBufferCapacity = 8)
    private val supervisorJob = SupervisorJob(parent = parent.coroutineContext.job)
    private val scope = parent + supervisorJob
    private val mutex = Mutex()

    init {
        checkNotNull(parent.coroutineContext[CoroutineDispatcher.Key]) { "You must specify a CoroutineDispatcher for the Loop." }

        val effects = LoopBuilder<State, Event>()
            .apply(action)
            .make()

        for (effect in effects) {
            flow
                .let(effect)
                .onEach(this::send)
                .launchIn(scope)
        }

        freeze()

        val sentInitial = flow.tryEmit(LoopSnapshot(initial, null, true))
        check(sentInitial)
    }

    fun sendUndispatched(event: Event) = scope.launch(start = CoroutineStart.UNDISPATCHED) {
        send(event)
    }

    suspend fun send(event: Event) = mutex.withLock {
        val old = flow.replayCache.first()

        if (!old.isActive) {
            throw IllegalStateException("Flow has been closed.")
        }

        val newState = reducer.reduce(old.state, event)
        flow.emit(LoopSnapshot(newState, event, true))
    }

    fun close() = parent.launch(start = CoroutineStart.ATOMIC) {
        mutex.withLock {
            supervisorJob.cancel()
            val lastState = flow.replayCache.first()
            flow.emit(LoopSnapshot(lastState.state, null, false))
        }
    }
}
