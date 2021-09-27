package me.sphere.flowredux

import app.cash.turbine.test
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.test.TestCoroutineDispatcher
import me.sphere.flowredux.assertion.AssertionReceiver
import me.sphere.flowredux.assertion.AssertionStoreBuilder
import me.sphere.flowredux.assertion.ExpectNoMoreItemsStep

fun <T> nonTerminalFlowOf(vararg items: T) = channelFlow<T> {
    items.forEach { offer(it) }

    awaitClose { }
}

@ExperimentalCoroutinesApi
@FlowPreview
@kotlin.time.ExperimentalTime
suspend fun <S, A> CoroutineScope.testStore(
    dispatcher: TestCoroutineDispatcher,
    store: Store<S, A>,
    assertionStoreBuilder: suspend AssertionStoreBuilder<S, A>.() -> Unit
) {

    val assertion = AssertionStoreBuilder(store)
    assertion.assertionStoreBuilder()

    val steps = assertion.steps
    launch(dispatcher) {
        store.state.test {

            val turbine = this
            assertion.steps.forEach { step ->
                step.assertion.assert(
                    AssertionReceiver.AssertionReceiverFlowTurbine(turbine)
                )
            }
        }
    }

    steps.find { it is ExpectNoMoreItemsStep }?.let {
        launch {
            delay(1000)
            dispatcher.advanceUntilIdle()
        }
    }

    steps.forEach { step ->
        step.input.run()
    }
}
