package me.sphere.appcore.utils

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import me.sphere.test.Tests
import me.sphere.test.support.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class FlowUtilsTests: Tests() {
    @Test
    fun delay_shouldPassthroughValuesAndCompletion() = runTesting {
        val channel = Channel<String>(Channel.BUFFERED)
        val flow = channel
            .receiveAsFlow()
            .timeout(10000) { RuntimeException("") }

        flow.test {
            channel.send("initial")
            assertEquals("initial", actual = expectItem())

            channel.offer("second")
            assertEquals("second", actual = expectItem())

            channel.close(null)
            expectComplete()
        }
    }

    @Test
    fun delay_shouldPassthroughException() = runTesting {
        val CustomException = object: RuntimeException() {}
        val TimeoutException = object: RuntimeException() {}

        val flow = flow<Unit> { throw CustomException }
            .timeout(10000) { TimeoutException }

        flow.test {
            assertEquals(CustomException, expectError())
        }
    }

    @Test
    fun delay_shouldTimeout() = runTesting {
        val channel = Channel<Unit>(Channel.BUFFERED)
        val TimeoutException = object: RuntimeException() {}

        val flow = channel
            .receiveAsFlow()
            .timeout(1) { TimeoutException }

        flow.test {
            assertEquals(TimeoutException, expectError())
        }

        channel.close(null)
    }
}
