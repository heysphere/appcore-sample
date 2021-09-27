package me.sphere.flowredux

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class Store<State, Action>(
    private val initialState: State,
    private val reducer: Reducer<State, Action>,
    private val dispatcher: CoroutineDispatcher,
    initialAction: List<Action>? = emptyList()
) {

    constructor(
        initialState: State,
        reducer: Reducer<State, Action>,
        dispatcher: CoroutineDispatcher,
        initialAction: Action?
    ) : this(
        initialState,
        reducer,
        dispatcher,
        listOfNotNull(initialAction)
    )

    private val mutableState = MutableStateFlow(initialState)

    val state: Flow<State>
        get() = mutableState

    private var scopeCollectionJob: Job? = null
    private val supervisorJob = SupervisorJob()

    init {
        initialAction?.forEach(::dispatch)
    }

    fun dispatch(action: Action) {
        assertMainThread()

        val (newState, effect) = reducer.reduce(mutableState.value, action)
        mutableState.value = newState

        GlobalScope.launch(dispatcher) {
            try {
                effect.flow.onEach {
                    withContext(dispatcher) {
                        dispatch(it)
                    }
                }
                    .launchIn(this + supervisorJob)
            } catch (ex: CancellationException) {
                // Ignore
            }
        }
    }

    // To be reviewed if we really need this
    internal fun <LocalState, LocalAction> scope(
        toLocalState: (State) -> LocalState,
        fromLocalAction: (LocalAction) -> Action,
        coroutineScope: CoroutineScope
    ): Store<LocalState, LocalAction> {
        val localStore = Store(
            toLocalState(mutableState.value),
            reducer = Reducer<LocalState, LocalAction> { _, action ->
                dispatch(fromLocalAction(action))
                Result(toLocalState(mutableState.value), emptyEffect())
            },
            dispatcher
        )

        localStore.scopeCollectionJob = coroutineScope.launch(Dispatchers.Unconfined) {
            mutableState.collect { newValue ->
                localStore.mutableState.value = toLocalState(newValue)
            }
        }

        return localStore
    }

    fun cancel() {
        scopeCollectionJob?.cancel()
        supervisorJob.cancel()
    }
}
