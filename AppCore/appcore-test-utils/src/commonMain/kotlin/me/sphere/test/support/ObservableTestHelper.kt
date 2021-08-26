package me.sphere.test.support

import kotlinx.atomicfu.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import me.sphere.test.Tests

fun <U> Tests.flowTest(
    action: FlowTestHelper<U>.() -> Unit
) = action(FlowTestHelper(defaultScope))

sealed class FlowStatus {
    object Unknown: FlowStatus()
    object Started: FlowStatus()
    object Completed: FlowStatus()
    class Failed(val throwable: Throwable): FlowStatus() {
        override fun toString(): String = "Failed because $throwable"
    }
}

class FlowTestHelper<U>(private val defaultScope: CoroutineScope) {
    private val state = atomic(listOf<U>())
    private val statusRef = atomic<FlowStatus>(FlowStatus.Unknown)

    val values: List<U>
        get() = state.value

    val status: FlowStatus
        get() = statusRef.value

    @OptIn(ExperimentalCoroutinesApi::class)
    fun launch(flow: Flow<U>, scope: CoroutineScope = defaultScope) = flow
        .onEach { newValue -> state.update { it + listOf(newValue) } }
        .onStart { statusRef.value = FlowStatus.Started }
        .onCompletion {
            val finalValue = if (it != null) FlowStatus.Failed(it) else FlowStatus.Completed
            statusRef.value = finalValue
        }
        .catch {}
        .launchIn(scope)
}
