package me.sphere.appcore.utils.loop

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import me.sphere.appcore.utils.Atomic
import me.sphere.appcore.utils.flatMapOnce

typealias LoopEffect<State, Event> = (Flow<LoopSnapshot<State, Event>>) -> Flow<Event>

fun <State, Event> loopBuilder(action: LoopBuilder<State, Event>.() -> Unit): LoopBuilder<State, Event> {
    return LoopBuilder<State, Event>().apply(action)
}

@OptIn(ExperimentalCoroutinesApi::class)
class LoopBuilder<State, Event> {
    private var items: MutableList<LoopEffect<State, Event>> = mutableListOf()

    fun make(): List<LoopEffect<State, Event>> = items.toList()

    fun merge(effects: List<LoopEffect<State, Event>>) {
        items.addAll(effects)
    }

    fun merge(builder: LoopBuilder<State, Event>) {
        items.addAll(builder.make())
    }

    fun custom(effect: LoopEffect<State, Event>) {
        items.add { snapshots -> effect(snapshots) }
    }

    /**
     * Run the given effect when the loop has emitted its initial state.
     */
    fun whenInitialized(effect: (State) -> Flow<Event>) {
        items.add { snapshots -> snapshots.flatMapOnce { effect(it.state) } }
    }

    /**
     * Run the given effect whenever the first time [predicate] returns `true` after any number of `false`s.
     *
     * Any outstanding effect is cancelled as soon as [predicate] returns `false`.
     *
     * e.g. Given this sequence of [predicate] output over time:
     * ```
     * false false false TRUE TRUE TRUE TRUE TRUE TRUE false false TRUE TRUE TRUE TRUE TRUE ................
     *                    *--------- effect -----------[X]
     *                                     (cancelled if not completed)
     *                                                              *---- effect ----------->
     * ```
     */
    fun whenBecomesTrue(predicate: (State) -> Boolean, effect: (State) -> Flow<Event>) {
        firstValueAfterEveryNull(
            mapper = { if (predicate(it)) it else null },
            effect = { state, _ -> effect(state) }
        )
    }

    /**
     * Run the given effect whenever the first time [mapper] returns a non-null value after any number of nulls.
     *
     * Any outstanding effect is cancelled as soon as [mapper] returns null.
     *
     * e.g. Given this sequence of [mapper] output over time:
     * ```
     * null null null VALUE VALUE VALUE VALUE VALUE VALUE null null VALUE VALUE VALUE VALUE VALUE ................
     *                  *--------- effect ----------------[X]
     *                                     (cancelled if not completed)
     *                                                                *---- effect ---------------->
     * ```
     */
    fun <Control> firstValueAfterEveryNull(mapper: (State) -> Control?, effect: (Control, State) -> Flow<Event>) {
        items.add { snapshots ->
            val wasNull = Atomic(true)

            return@add snapshots
                .map { it.state }
                .transform<State, Pair<Control, State>?> { state ->
                    val control = mapper(state)

                    if (control != null) {
                        if (wasNull.value) {
                            wasNull.value = false
                            emit(Pair(control, state))
                        }
                    } else {
                        if (!wasNull.value) {
                            wasNull.value = true
                            emit(null)
                        }
                    }
                }
                .flatMapLatest { pair ->
                    pair?.let { effect(it.first, it.second) } ?: emptyFlow()
                }
        }
    }

    /**
     * Run the given effect whenever [mapper] returns a new value that is distinct from the previous one. The evaluation
     * is based on [Any.equals].
     *
     * Any outstanding effect is cancelled when [mapper] returns a new distinct value, or when [mapper] returns null.
     * In other words, it has similar semantics to the concatenation of [Flow.distinctUntilChanged] and
     * [Flow.flatMapLatest].
     */
    fun <Control> skippingRepeated(mapper: (State) -> Control?, effect: (Control, State) -> Flow<Event>) {
        items.add { snapshots ->
            val previous = Atomic<Control?>(null)

            snapshots
                .transform { snapshot ->
                    val control = mapper(snapshot.state)
                    if (previous.value != control) {
                        previous.value = control
                        emit(Pair(control, snapshot.state))
                    }
                }
                .flatMapLatest { (control, state) ->
                    if (control != null) effect(control, state) else emptyFlow()
                }
        }
    }
}

