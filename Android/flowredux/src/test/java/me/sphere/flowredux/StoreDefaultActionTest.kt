package me.sphere.flowredux

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.setMain
import me.sphere.flowredux.Effect.Companion.withoutEffect
import me.sphere.flowredux.android.StoreViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

class StoreDefaultActionTest {

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @Before
    fun before() {
        Dispatchers.setMain(TestCoroutineDispatcher())
    }

    @Test
    fun `given no default action when consuming the state viewModel it should not send an action`() {
        val viewModel = StubViewModel()

        viewModel.state.observeForever {}

        assert(viewModel.receivedActions.isEmpty())
        assert(viewModel.receivedStates.isEmpty())
    }

    @Test
    fun `given default action when consuming the state viewModel it should send an action`() {
        val viewModel = StubViewModel(1)

        viewModel.state.observeForever {}

        assert(viewModel.receivedActions == listOf(1))
        assert(viewModel.receivedStates == listOf("1"))
    }

    @Test
    fun `given default action when sending an action it should send two actions`() {
        val viewModel = StubViewModel(1)

        viewModel.sendAction(2)

        viewModel.state.observeForever {}

        assert(viewModel.receivedActions == listOf(1, 2))
        assert(viewModel.receivedStates == listOf("1", "2"))
    }

    @Test
    fun `given default action when sending an action without state observation it should send two actions`() {
        val viewModel = StubViewModel(1)
        viewModel.sendAction(2)

        assert(viewModel.receivedActions == listOf(1, 2))
        assert(viewModel.receivedStates == listOf("1", "2"))
    }
}

private class StubViewModel(
    initialAction: Int? = null
) : StoreViewModel<String, Int>() {

    val receivedActions = mutableListOf<Int>()
    val receivedStates = mutableListOf<String>()

    override val store: Store<String, Int> = Store(
        initialState = "",
        reducer = object : Reducer<String, Int> {
            override fun reduce(state: String, action: Int): Result<String, Int> {
                receivedActions.add(action)

                return action.toString().let { newState ->
                    receivedStates.add(newState)
                    newState.withoutEffect()
                }
            }
        },
        dispatcher = TestCoroutineDispatcher(),
        initialAction = initialAction
    )
}
