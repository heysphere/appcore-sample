package me.sphere.appcore.utils.loop

interface LoopDefinition<State, Event, Environment> {
    val reducer: LoopReducer<State, Event>
    fun effects(environment: Environment): LoopBuilder<State, Event>
}
