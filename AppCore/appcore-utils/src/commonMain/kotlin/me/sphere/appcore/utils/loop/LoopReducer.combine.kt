package me.sphere.appcore.utils.loop

fun <State, Event> LoopReducer.Companion.combine(
    vararg reducers: LoopReducer<State, Event>
): LoopReducer<State, Event> = LoopReducer { state, event ->
    reducers.fold(state) { current, reducer -> reducer.reduce(current, event) }
}
