package me.sphere.flowredux.assertion

import org.junit.Assert.assertEquals

class ReceiveStep<State>(
    override val assertion: ReceiveStepAssertion<State>
) : Step<State, ReceiveStepInput, ReceiveStepAssertion<State>> {

    override val input: ReceiveStepInput = ReceiveStepInput
}

object ReceiveStepInput : Step.Input {

    override suspend fun run() {}
}

class ReceiveStepAssertion<State>(
    private val expectedState: State
) : Step.Assertion<State> {

    override suspend fun assert(assertionReceiver: AssertionReceiver<State>) = when (assertionReceiver) {
        is AssertionReceiver.AssertionReceiverFlowTurbine -> {
            assertEquals("States are not the same", expectedState, assertionReceiver.turbine.expectItem())
        }
        is AssertionReceiver.AssertionReceiverViewModel -> {
            assertEquals("States are not the same", expectedState, assertionReceiver.state)
        }
    }
}
