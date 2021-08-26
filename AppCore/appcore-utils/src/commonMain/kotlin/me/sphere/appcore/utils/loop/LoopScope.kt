package me.sphere.appcore.utils.loop

import kotlinx.coroutines.flow.map

/**
 * [LoopScope] defines the standard requirements for integrating a smaller [Loop] and its effects into a larger one,
 * allowing complicated state management to be decomposed into smaller, individually testable [Loop]s.
 */
interface LoopScope<RootState, RootEvent, LocalState, LocalEvent> {
    fun extractState(state: RootState): LocalState
    fun extractEvent(event: RootEvent): LocalEvent?

    fun merge(state: RootState, local: LocalState): RootState
    fun wrap(event: LocalEvent): RootEvent
}

fun <RS, RE, LS, LE> LoopScope<RS, RE, LS, LE>.pullback(
    reducer: LoopReducer<LS, LE>
) = LoopReducer<RS, RE> { state, event ->
    val localEvent = extractEvent(event)

    return@LoopReducer if (localEvent != null) {
        merge(state, reducer.reduce(extractState(state), localEvent))
    } else {
        state
    }
}

fun <RS, RE, LS, LE> LoopScope<RS, RE, LS, LE>.pullback(
    builder: LoopBuilder<LS, LE>
): List<LoopEffect<RS, RE>> {
    val localEffects = builder.make()

    return localEffects.map { localEffect ->
        { transitions ->
            transitions
                .map { snapshot ->
                    LoopSnapshot(
                        state = extractState(snapshot.state),
                        lastEvent = snapshot.lastEvent?.let { extractEvent(it) },
                        isActive = snapshot.isActive
                    )
                }
                .let(localEffect)
                .map(::wrap)
        }
    }
}
