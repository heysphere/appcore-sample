package me.sphere.flowredux.assertion

import me.sphere.flowredux.Store

class AssertionStoreBuilder<State, Action>(
    private val store: Store<State, Action>
) {
    internal val steps: MutableList<Step<State, *, out Step.Assertion<State>>> = mutableListOf()

    fun send(action: Action, block: () -> State) {
        steps.add(
            SendStep(
                SendStepInput { store.dispatch(action) },
                SendStepAssertion(block())
            )
        )
    }

    fun receive(block: () -> State) {
        steps.add(
            ReceiveStep(
                ReceiveStepAssertion(block())
            )
        )
    }

    fun expectNoMoreItems() {
        steps.add(ExpectNoMoreItemsStep())
    }

    fun customAssertion(assertion: () -> Unit) {
        steps.add(
            CustomAssertionStep(assertion)
        )
    }
}
