package me.sphere.flowredux

data class Result<State, Action>(val state: State, val effect: Effect<Action>)

fun interface Reducer<State, Action> {

    fun reduce(state: State, action: Action): Result<State, Action>

    fun <ParentState, ParentAction> transform(
        toLocalState: (ParentState) -> State,
        toLocalAction: (ParentAction) -> Action?,
        toParentState: (ParentState, State) -> ParentState,
        toParentAction: (Action) -> ParentAction
    ) = Reducer<ParentState, ParentAction> { state, action ->
        val localAction = toLocalAction(action)
        if (localAction == null) {
            Result(state, emptyEffect())
        } else {
            reduce(state = toLocalState(state), action = localAction).let {
                Result(
                    state = toParentState(state, it.state),
                    effect = it.effect.map(toParentAction)
                )
            }
        }
    }

    fun combine(other: Reducer<State, Action>) = combine(this, other)

    companion object {

        fun <State, Action> combine(vararg reducers: Reducer<State, Action>): Reducer<State, Action> {

            return Reducer { state, action ->
                reducers.fold(Result(state, emptyEffect())) { result, reducer ->
                    val (currentValue, currentEffect) = result
                    val (newValue, newEffect) = reducer.reduce(currentValue, action)
                    currentEffect.merge(newEffect)
                    Result(newValue, currentEffect)
                }
            }
        }
    }
}
