package me.sphere.flowredux

import kotlinx.coroutines.flow.*

/**
 * Effect is an asynchronous operation which can be started from your reducer
 * within your store. Usually this is useful if we want to perform some blocking
 * long running operation which we want to delegate to a background thread,
 * such as performing a network call.
 */
class Effect<Action>(
    internal var flow: Flow<Action>
) {

    fun <T> map(transform: (Action) -> T): Effect<T> = Effect(flow.map { transform(it) })

    fun merge(vararg effects: Effect<Action>) {
        flow = flowOf(flow, *effects.map { it.flow }.toTypedArray()).flattenMerge()
    }

    companion object {

        fun <State, Action> State.withoutEffect() = Result<State, Action>(this, emptyEffect())

        fun <State, Action> State.withSideEffect(block: suspend () -> Unit) = Result<State, Action>(
            this,
            fireAndForgetEffect(block)
        )

        fun <State, Action> State.withEffect(block: suspend () -> Action) = Result(this, effect(block))

        fun <State, Action> State.withEffect(effect: Action) = Result(this, Effect(flowOf(effect)))

        fun <State, Action> State.withEffect(effect: Effect<Action>) = Result(this, effect)

        fun <State, Action> State.withEffect(flow: Flow<Action>) = Result(this, Effect(flow))

        fun <Action> effect(block: suspend () -> Action): Effect<Action> {
            return Effect(
                flow {
                    emit(block())
                }
            )
        }
    }
}
