package me.sphere.appcore

import app.cash.turbine.test
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import me.sphere.appcore.dataSource.DataSource
import me.sphere.appcore.dataSource.combine
import me.sphere.appcore.dataSource.map
import me.sphere.appcore.dataSource.toListDataSource
import me.sphere.appcore.utils.Atomic
import me.sphere.test.support.runTesting
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal class ListDataSourceConversionTests {
    private val lhs = FakeDataSource<List<String>>()
    private val composition = lhs.toListDataSource()

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

            lhs.flow.value = Value(listOf("lhs-0"))
            assertEquals(Value(listOf("lhs-0")), actual = expectItem())

            lhs.flow.value = Value(listOf("lhs-0", "lhs-1"))
            assertEquals(Value(listOf("lhs-0", "lhs-1")), actual = expectItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun output_should_change_to_loading_if_lhs_becomes_loading() = runTesting {
        lhs.flow.value = Value(listOf("lhs-0"))

        composition.state.test {
            assertEquals(Value(listOf("lhs-0")), actual = expectItem())

            lhs.flow.value = Loading
            assertEquals(Loading, actual = expectItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun output_should_change_to_failed_if_lhs_becomes_failed() = runTesting {
        lhs.flow.value = Value(listOf("lhs-0"))

        composition.state.test {
            assertEquals(Value(listOf("lhs-0")), actual = expectItem())

            lhs.flow.value = Failed(IllegalStateException())
            assertEquals(Failed::class, actual = expectItem()::class)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
