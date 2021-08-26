package me.sphere.test

import app.cash.turbine.FlowTurbine
import kotlinx.atomicfu.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import me.sphere.appcore.utils.loop.*
import kotlin.coroutines.coroutineContext
import kotlin.test.*

abstract class LoopTests<Definition: LoopDefinition<State, Event, Environment>, State, Event, Environment>(
    private val definition: Definition
): Tests() {
    abstract val environment: Environment

    private val _state = atomic(listOf<State>())
    val state: List<State> get() = _state.value

    private val loop = atomic<Loop<State, Event>?>(null)

    /**
     * Given the receiver as the initial state, start a coroutine and setup a [Loop] for running the test.
     */
    fun State.runTesting(body: suspend Loop<State,Event>.(State) -> Unit) = me.sphere.test.support.runTesting {
        val loop = Loop(
            parent = CoroutineScope(coroutineContext),
            initial = this@runTesting,
            reducer = definition.reducer
        ) { merge(definition.effects(environment)) }

        loop.body(this@runTesting)
        loop.close()
    }

    @Deprecated("Use `State.runTesting()` to write coroutine based test instead.")
    fun initialize(state: State) {
        if (loop.value != null) {
            fail("Loop has been initialized. Cannot initialize again.")
        }

        val loop = Loop(
            parent = defaultScope,
            initial = state,
            reducer = definition.reducer
        ) { merge(definition.effects(environment)) }

        loop.state
            .onEach { new -> _state.update { it + listOf(new) } }
            .launchIn(defaultScope)

        busyWait {
            assertTrue(this@LoopTests.state.isNotEmpty(), "Expected initial state from the Loop. Found none.")
        }
    }

    fun sendAndWait(event: Event) {
        val loop = this.loop.value ?: fail("Loop is uninitialized.")
        val job = defaultScope.launch { loop.send(event) }
        busyWait { assertTrue(job.isCompleted, "Event should have been sent") }
    }

    @AfterTest
    override fun cleanup() {
        val loop = this.loop.getAndSet(null)

        GlobalScope.launch { loop?.close() }
        super.cleanup()
    }
}
