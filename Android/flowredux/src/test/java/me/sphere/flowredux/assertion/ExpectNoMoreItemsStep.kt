package me.sphere.flowredux.assertion

class ExpectNoMoreItemsStep<State> : Step<State, Step.Input, ExpectNoMoreItemsAssertion<State>> {

    override val input: Step.Input = object : Step.Input {
        override suspend fun run() {
        }
    }

    override val assertion: ExpectNoMoreItemsAssertion<State> = ExpectNoMoreItemsAssertion()
}

class ExpectNoMoreItemsAssertion<State> : Step.Assertion<State> {

    override suspend fun assert(assertionReceiver: AssertionReceiver<State>) {
        when (assertionReceiver) {
            is AssertionReceiver.AssertionReceiverFlowTurbine -> {
                assertionReceiver.turbine.expectComplete()
            }
            is AssertionReceiver.AssertionReceiverViewModel -> {
                throw RuntimeException("received item while it shouldn't")
            }
        }
    }
}
