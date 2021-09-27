package me.sphere.flowredux.assertion

import org.junit.Assert.assertEquals

class SendStep<State>(
    override val input: SendStepInput,
    override val assertion: SendStepAssertion<State>
) : Step<State, SendStepInput, SendStepAssertion<State>>

class SendStepInput(
    private val sendActionBody: () -> Unit
) : Step.Input {

    override suspend fun run() {
        sendActionBody()
    }
}

class SendStepAssertion<State>(
    private val expectedState: State
) : Step.Assertion<State> {

    override suspend fun assert(assertionReceiver: AssertionReceiver<State>) = when (assertionReceiver) {
        is AssertionReceiver.AssertionReceiverFlowTurbine ->
            assertEquals(expectedState, assertionReceiver.turbine.expectItem())
        is AssertionReceiver.AssertionReceiverViewModel ->
            assertEquals(expectedState, assertionReceiver.state)
    }
}
