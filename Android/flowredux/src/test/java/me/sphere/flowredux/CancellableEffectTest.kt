package me.sphere.flowredux

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import me.sphere.flowredux.Effect.Companion.effect
import me.sphere.flowredux.Effect.Companion.withEffect
import me.sphere.flowredux.Effect.Companion.withoutEffect
import org.junit.Test

@ExperimentalCoroutinesApi
@FlowPreview
@kotlin.time.ExperimentalTime
internal class CancellableEffectTest {

    private val testDispatcher = TestCoroutineDispatcher()

    @Test
    fun `cancel a simple effect`() = runBlocking {

        val effectHelper = EffectCancellationHelper {
            delay(100)
            33
        }

        val store = Store(
            initialState = "",
            reducer = Reducer<String, Int> { state, action ->
                val newState = state + action
                when (action) {
                    1 -> Result(newState, effectHelper.effect.cancellable(77))
                    5 -> newState.cancel(77)
                    else -> newState.withoutEffect()
                }
            },
            dispatcher = testDispatcher
        )

        testStore(testDispatcher, store) {
            receive { "" }
            send(0) { "0" }
            send(1) { "01" }
            send(2) { "012" }
            send(5) { "0125" }
            send(6) { "01256" }
            customAssertion { effectHelper.assertBlockNotExecuted() }
            send(7) { "012567" }
            expectNoMoreItems()
        }
    }

    @Test
    fun `cancel all in flight effects except the last`() = runBlocking {
        val store = Store(
            initialState = "",
            reducer = Reducer<String, Int> { state, action ->
                val newState = state + action
                val effectHelper = EffectCancellationHelper {
                    delay(100)
                    33
                }

                when (action) {
                    33 -> state.withoutEffect() // no op just terminate after that
                    else -> Result(newState, effectHelper.effect.cancellable(77, true))
                }
            },
            dispatcher = testDispatcher
        )

        testStore(testDispatcher, store) {
            receive { "" }
            send(0) { "0" }
            send(1) { "01" }
            send(2) { "012" }
            send(5) { "0125" }
            send(6) { "01256" }
            send(7) { "012567" }
            receive { "01256733" }
            expectNoMoreItems()
        }
    }

    @Test
    fun `execute a simple cancellable effect`() = runBlocking {
        val store = Store(
            initialState = "",
            reducer = Reducer<String, Int> { state, action ->
                val newState = state + action
                when (action) {
                    5 -> newState.withEffect(33).cancellable(77)
                    else -> newState.withoutEffect()
                }
            },
            dispatcher = testDispatcher
        )

        testStore(testDispatcher, store) {
            receive { "" }
            send(0) { "0" }
            send(1) { "01" }
            send(2) { "012" }
            send(5) { "0125" }
            receive { "012533" }
            send(6) { "0125336" }
            send(7) { "01253367" }
            expectNoMoreItems()
        }
    }

    @Test
    fun `execute a non terminal cancellable effect`() = runBlocking {
        val store = Store(
            initialState = "",
            reducer = Reducer<String, Int> { state, action ->
                val newState = state + action
                when (action) {
                    5 -> Result(newState, Effect(nonTerminalFlowOf(6, 7))).cancellable(88)
                    else -> newState.withoutEffect()
                }
            },
            dispatcher = testDispatcher
        )

        testStore(testDispatcher, store) {
            receive { "" }
            send(0) { "0" }
            send(1) { "01" }
            send(2) { "012" }
            send(5) { "0125" }
            receive { "01256" }
            receive { "012567" }
            send(8) { "0125678" }
            send(9) { "01256789" }
            expectNoMoreItems()
        }
    }

    @Test
    fun `cancel all in flight non terminal effects except the last`() = runBlocking {
        val store = Store(
            initialState = "",
            reducer = Reducer<String, Int> { state, action ->
                val newState = state + action
                val effect = Effect(
                    flow {
                        delay(100)
                        emitAll(nonTerminalFlowOf(33 + action))
                    }
                )

                when (action) {
                    40 -> state.withoutEffect() // no op just terminate after that
                    else -> Result(newState, effect.cancellable(77, true))
                }
            },
            dispatcher = testDispatcher
        )

        testStore(testDispatcher, store) {
            receive { "" }
            send(0) { "0" }
            send(1) { "01" }
            send(2) { "012" }
            send(5) { "0125" }
            send(6) { "01256" }
            send(7) { "012567" }
            receive { "01256740" }
            expectNoMoreItems()
        }
    }
}

private class EffectCancellationHelper<Action>(block: suspend () -> Action) {

    private var didBlockGetExecuted = false

    val effect = effect {
        block().also {
            didBlockGetExecuted = true
        }
    }

    fun assertBlockNotExecuted() {
        require(!didBlockGetExecuted)
    }
}
