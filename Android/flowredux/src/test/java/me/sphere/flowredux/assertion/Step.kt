package me.sphere.flowredux.assertion

import app.cash.turbine.FlowTurbine

interface Step<State, Input : Step.Input, Assertion : Step.Assertion<State>> {

    val input: Input

    val assertion: Assertion

    interface Input {
        suspend fun run()
    }

    interface Assertion<State> {
        suspend fun assert(assertionReceiver: AssertionReceiver<State>)
    }
}

sealed class AssertionReceiver<State> {

    class AssertionReceiverFlowTurbine<State>(
        val turbine: FlowTurbine<State>
    ) : AssertionReceiver<State>()

    class AssertionReceiverViewModel<State>(
        val state: State
    ) : AssertionReceiver<State>()
}
