package me.sphere.appcore

import app.cash.turbine.test
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import me.sphere.appcore.dataSource.DataSource
import me.sphere.appcore.dataSource.combine
import me.sphere.appcore.utils.Atomic
import me.sphere.test.support.runTesting
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal class DataSourceCombineTests {
    private val lhs = FakeDataSource<String>()
    private val rhs = MutableStateFlow("rhs-0")
    private val composition = lhs.combine(rhs) { left, right -> DataSource.State.Value("$left:$right") }

    @Test
    fun should_call_refreshNow_on_lhs() {
        val invoked = Atomic(false)
        lhs.stubRefreshNow.value = { invoked.value = true }
        composition.refreshNow()
        assertTrue(invoked.value)
    }

    @Test
    fun should_change_when_lhs_changes() = runTesting {
        composition.state.test {
            assertEquals(DataSource.State.Loading, actual = expectItem())

            lhs.flow.value = Value("lhs-0")
            assertEquals(Value("lhs-0:rhs-0"), actual = expectItem())

            lhs.flow.value = Value("lhs-1")
            assertEquals(Value("lhs-1:rhs-0"), actual = expectItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun should_change_when_rhs_changes() = runTesting {
        lhs.flow.value = Value("lhs-0")

        composition.state.test {
            assertEquals(Value("lhs-0:rhs-0"), actual = expectItem())

            rhs.value = "rhs-1"
            assertEquals(Value("lhs-0:rhs-1"), actual = expectItem())

            rhs.value = "rhs-2"
            assertEquals(Value("lhs-0:rhs-2"), actual = expectItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun output_should_stay_loading_when_lhs_is_loading() = runTesting {
        composition.state.test {
            assertEquals(Loading, actual = expectItem())

            rhs.value = "rhs-1"
            assertEquals(Loading, actual = expectItem())

            rhs.value = "rhs-2"
            assertEquals(Loading, actual = expectItem())

            lhs.flow.value = Value("lhs-0")
            assertEquals(Value("lhs-0:rhs-2"), actual = expectItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun output_should_stay_failed_when_lhs_is_failed() = runTesting {
        lhs.flow.value = Failed(IllegalStateException())
        composition.state.test {
            assertEquals(Failed::class, actual = expectItem()::class)

            rhs.value = "rhs-1"
            assertEquals(Failed::class, actual = expectItem()::class)

            rhs.value = "rhs-2"
            assertEquals(Failed::class, actual = expectItem()::class)

            lhs.flow.value = Value("lhs-0")
            assertEquals(Value("lhs-0:rhs-2"), actual = expectItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun output_should_change_to_loading_if_lhs_becomes_loading() = runTesting {
        lhs.flow.value = Value("lhs-0")

        composition.state.test {
            assertEquals(Value("lhs-0:rhs-0"), actual = expectItem())

            lhs.flow.value = Loading
            assertEquals(Loading, actual = expectItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun output_should_change_to_failed_if_lhs_becomes_failed() = runTesting {
        lhs.flow.value = Value("lhs-0")

        composition.state.test {
            assertEquals(Value("lhs-0:rhs-0"), actual = expectItem())

            lhs.flow.value = Failed(IllegalStateException())
            assertEquals(Failed::class, actual = expectItem()::class)
            cancelAndIgnoreRemainingEvents()
        }
    }
    @Test
    fun rhs_throwing_exception_should_complete_the_composed_flow_exceptionally() = runTesting {
        lhs.flow.value = Value("world")
        val closableRhs = Channel<String>()
        val composition = lhs.combine(closableRhs.receiveAsFlow()) { left, right -> DataSource.State.Value("$left:$right") }

        composition.state.test {
            closableRhs.send("hello")
            assertEquals(Value("world:hello"), actual = expectItem())

            closableRhs.close(IllegalArgumentException())
            assertEquals(IllegalArgumentException::class, actual = expectError()::class)
        }
    }
}
