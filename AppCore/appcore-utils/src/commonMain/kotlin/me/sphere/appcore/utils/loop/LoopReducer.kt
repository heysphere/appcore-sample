package me.sphere.appcore.utils.loop

fun interface LoopReducer<State, Event> {
    fun reduce(state: State, event: Event): State

    companion object
}
