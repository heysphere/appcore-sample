package me.sphere.flowredux

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import me.sphere.flowredux.Effect.Companion.withEffect
import me.sphere.flowredux.Effect.Companion.withoutEffect
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

@ExperimentalCoroutinesApi
@FlowPreview
@kotlin.time.ExperimentalTime
internal class StoreTest {

    @Test
    fun `test simple reducer`() = runBlocking {
        val testDispatcher = TestCoroutineDispatcher()

        val store = Store(
            initialState = 0,
            reducer = Reducer<Int, Int> { state, action -> (state + action).withoutEffect() },
            dispatcher = testDispatcher
        )

        testStore(testDispatcher, store) {
            send(0) { 0 }
            send(1) { 1 }
            send(3) { 4 }
            expectNoMoreItems()
        }
    }

    @Test
    fun `test simple side effect`() = runBlocking {
        val testDispatcher = TestCoroutineDispatcher()

        val store = Store(
            initialState = "",
            reducer = Reducer<String, Int> { state, action ->
                if (action == 1) {
                    val newState = state + action
                    val effect = Effect.effect { action * 2 }
                    Result(newState, effect)
                } else {
                    Result(state + action, emptyEffect())
                }
            },
            dispatcher = testDispatcher
        )

        testStore(testDispatcher, store) {
            receive { "" }
            send(1) { "1" }
            receive { "12" }
            send(3) { "123" }
            expectNoMoreItems()
        }
    }

    @Test
    fun `test simple side effect with flow`() = runBlocking {
        val testDispatcher = TestCoroutineDispatcher()

        val store = Store(
            initialState = "",
            reducer = Reducer<String, Int> { state, action ->
                println("reduce ====> state $state action $action")
                if (action == 1) {
                    (state + action).withEffect(
                        flow {
                            emit(2)
                            emit(3)
                        }
                    )
                } else {
                    (state + action).withoutEffect()
                }
            },
            dispatcher = testDispatcher
        )

        testStore(testDispatcher, store) {
            receive { "" }
            send(1) { "1" }
            receive { "12" }
            receive { "123" }
            send(3) { "1233" }
        }
    }

    @Test
    fun `simple side effect with non terminal flow`() = runBlocking {
        val testDispatcher = TestCoroutineDispatcher()

        val store = Store(
            initialState = "",
            reducer = Reducer<String, Int> { state, action ->
                println("reduce ====> state $state action $action")

                if (action == 1) {
                    Result(state + action, Effect(nonTerminalFlowOf(2, 3)))
                } else {
                    Result(state + action, emptyEffect())
                }
            },
            dispatcher = testDispatcher
        )

        testStore(testDispatcher, store) {
            receive { "" }
            send(1) { "1" }
            receive { "12" }
            receive { "123" }
            send(3) { "1233" }
        }
    }

    @Test
    fun `fire and forget side effect with non terminal flow`() = runBlocking {
        val testDispatcher = TestCoroutineDispatcher()

        val counter = AtomicInteger(0)
        val fireAndForgetEffect = fireAndForgetEffect<Int> {
            counter.incrementAndGet()
        }

        val store = Store(
            initialState = "",
            reducer = Reducer<String, Int> { state, action ->
                println("reduce ====> state $state action $action")

                if (action == 1) {
                    Result(state + action, fireAndForgetEffect)
                } else {
                    Result(state + action, emptyEffect())
                }
            },
            dispatcher = testDispatcher
        )

        testStore(testDispatcher, store) {
            receive { "" }
            send(1) { "1" }
            send(2) { "12" }
            customAssertion { assert(counter.get() == 1) }
        }
    }
}
