package me.sphere.appcore.utils

import app.cash.turbine.test
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import me.sphere.appcore.utils.loop.*
import me.sphere.test.Tests
import me.sphere.test.support.runTesting
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class LoopTests: Tests() {
    private val initial = "lorem"
    private val reducer = LoopReducer<String, Event> { state, event ->
        when (event) {
            is Event.Reset -> ""
            is Event.Append -> if (state.isNotEmpty()) "$state-${event.string}" else event.string
        }
    }

    sealed class Event {
        object Reset: Event()
        class Append(val string: String): Event()
    }

    @Test
    fun should_emit_initial_value() = runTesting {
        val loop = Loop(this, initial, reducer) {}
        val value = loop.state.first()
        assertEquals(initial, actual = value)

        loop.close()
    }

    @Test
    fun should_run_reducer_upon_receiving_events() = runTesting {
        val loop = Loop(this, initial, reducer) {}

        loop.state.take(3).test {
            assertEquals("lorem", actual = expectItem())

            loop.send(Event.Append("ipsum"))
            assertEquals("lorem-ipsum", actual = expectItem())

            loop.send(Event.Append("hello"))
            assertEquals("lorem-ipsum-hello", actual = expectItem())

            expectComplete()
        }

        loop.close()
    }

    @ExperimentalStdlibApi
    @Test
    fun whenBecomesTrue_should_run_effects_only_once_for_every_false_to_true_transition() = runTesting {
        val loop = Loop(this, initial, reducer) {
            whenBecomesTrue({ it.startsWith("lorem") }) {
                flowOf(Event.Append("whenBecomesTrue"))
            }
        }

        loop.state.take(7).test {
            assertEquals("lorem", actual = expectItem())
            assertEquals("lorem-whenBecomesTrue", actual = expectItem())

            loop.send(Event.Append("ipsum"))
            assertEquals("lorem-whenBecomesTrue-ipsum", actual = expectItem())

            loop.send(Event.Reset)
            assertEquals("", actual = expectItem())

            loop.send(Event.Append("lorem2"))
            assertEquals("lorem2", actual = expectItem())
            assertEquals("lorem2-whenBecomesTrue", actual = expectItem())

            loop.send(Event.Append("ipsum"))
            assertEquals("lorem2-whenBecomesTrue-ipsum", actual = expectItem())

            expectComplete()
        }

        loop.close()
    }

    @Test
    fun firstValueAfterEveryNull_should_run_effects_only_once_for_every_null_to_nonnull_transition() = runTesting {
        val loop = Loop(this, initial, reducer) {
            firstValueAfterEveryNull({ if (it.startsWith("lorem")) "whenBecomesTrue" else null }) { control, _ ->
                flowOf(Event.Append(control))
            }
        }

        loop.state.take(7).test {
            assertEquals("lorem", actual = expectItem())
            assertEquals("lorem-whenBecomesTrue", actual = expectItem())

            loop.send(Event.Append("ipsum"))
            assertEquals("lorem-whenBecomesTrue-ipsum", actual = expectItem())

            loop.send(Event.Reset)
            assertEquals("", actual = expectItem())

            loop.send(Event.Append("lorem2"))
            assertEquals("lorem2", actual = expectItem())
            assertEquals("lorem2-whenBecomesTrue", actual = expectItem())

            loop.send(Event.Append("ipsum"))
            assertEquals("lorem2-whenBecomesTrue-ipsum", actual = expectItem())

            expectComplete()
        }

        loop.close()
    }
}
