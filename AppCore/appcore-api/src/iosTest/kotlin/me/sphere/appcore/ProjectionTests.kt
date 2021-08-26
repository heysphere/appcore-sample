package me.sphere.appcore

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import me.sphere.appcore.utils.createSingleThreadDispatcher
import me.sphere.test.Tests
import me.sphere.test.support.flowTest
import kotlin.native.concurrent.isFrozen
import kotlin.test.Test

class ProjectionTests: Tests() {
    private val backgroundDispatcher = createSingleThreadDispatcher("ProjectionTest")

    @OptIn(ExperimentalCoroutinesApi::class)
    private val source = callbackFlow<List<TestData>> {
        val data = listOf(TestData(value = 1))
        require(!data.isFrozen)
        send(data)
        close()
    }

    @Test
    fun `observableByEmitter_should_not_freeze_received_values`() = flowTest<Boolean> {
        runBlocking {
            launch(source.map { it.isFrozen }, this)
        }

        busyWait {
            assertFalse(values.firstOrNull(), "received value is NOT frozen")
        }
    }

    @Test
    fun `Projection_should_freeze_received_values_automatically`() = flowTest<Boolean> {
        launch(source.asProjection().map { it.isFrozen }.flowOn(backgroundDispatcher))

        busyWait {
            assertTrue(values.firstOrNull(), "received value is frozen")
        }
    }

    @Test
    fun `ListProjection_should_freeze_received_values_automatically`() = flowTest<Boolean> {
        launch(source.asListProjection().map { it.isFrozen }.flowOn(backgroundDispatcher))

        busyWait {
            assertTrue(values.firstOrNull(), "received value is frozen")
        }
    }
}


private data class TestData(val value: Int)
