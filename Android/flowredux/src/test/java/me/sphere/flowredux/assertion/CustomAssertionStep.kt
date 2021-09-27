package me.sphere.flowredux.assertion

class CustomAssertionStep<State>(
    private val block: () -> Unit
) : Step<State, Step.Input, Step.Assertion<State>> {

    override val input: Step.Input = object : Step.Input {
        override suspend fun run() {
        }
    }
    override val assertion: Step.Assertion<State> = object : Step.Assertion<State> {

        override suspend fun assert(assertionReceiver: AssertionReceiver<State>) {
            block()
        }
    }
}
