package me.sphere.appcore

import app.cash.turbine.test
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import me.sphere.appcore.dataSource.DataSource
import me.sphere.appcore.dataSource.combine
import me.sphere.appcore.dataSource.map
import me.sphere.appcore.utils.Atomic
import me.sphere.test.support.runTesting
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal class DataSourceMapTests {
    private val lhs = FakeDataSource<String>()
    private val composition = lhs.map { "${it}:zzz" }

    @Test
    fun should_call_refreshNow_on_lhs() {
        val invoked = Atomic(false)
        lhs.stubRefreshNow.value = { invoked.value = true }
        composition.refreshNow()
        assertTrue(invoked.value)
    }

    @Test
    fun should_change_when_receiver_changes() = runTesting {
        composition.state.test {
            assertEquals(DataSource.State.Loading, actual = expectItem())

            lhs.flow.value = Value("lhs-0")
            assertEquals(Value("lhs-0:zzz"), actual = expectItem())

            lhs.flow.value = Value("lhs-1")
            assertEquals(Value("lhs-1:zzz"), actual = expectItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun output_should_change_to_loading_if_lhs_becomes_loading() = runTesting {
        lhs.flow.value = Value("lhs-0")

        composition.state.test {
            assertEquals(Value("lhs-0:zzz"), actual = expectItem())

            lhs.flow.value = Loading
            assertEquals(Loading, actual = expectItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun output_should_change_to_failed_if_lhs_becomes_failed() = runTesting {
        lhs.flow.value = Value("lhs-0")

        composition.state.test {
            assertEquals(Value("lhs-0:zzz"), actual = expectItem())

            lhs.flow.value = Failed(IllegalStateException())
            assertEquals(Failed::class, actual = expectItem()::class)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
