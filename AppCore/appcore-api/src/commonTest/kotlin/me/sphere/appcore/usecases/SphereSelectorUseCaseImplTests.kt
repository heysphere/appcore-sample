package me.sphere.appcore.usecases

import kotlinx.datetime.Instant
import me.sphere.appcore.stubs.*
import me.sphere.appcore.usecases.home.*
import me.sphere.test.DbTests
import me.sphere.test.support.flowTest
import me.sphere.sqldelight.*
import me.sphere.sqldelight.Sphere
import me.sphere.sqldelight.operations.OperationUtils
import me.sphere.test.support.TestLogger
import kotlin.test.Test

private val END_TIME = Instant.fromEpochSeconds(100000)

class SphereSelectorUseCaseImplTests: DbTests() {
    private val useCase = sphereSelectorUseCase(database, OperationUtils(database, TestLogger, StoreScope))

    @Test
    fun initialState() = flowTest<SphereSelector.AllSpheres> {
        launch(useCase.spheres)

        busyWait {
            val value = values.firstOrNull()
            assertEquals(value?.selectedSphereId, null)
            assertEquals(value?.spheres, emptyList())
            assertEquals(value?.status, SphereSelector.Status.Loading)
        }
    }

    @Test
    fun failure() = flowTest<SphereSelector.AllSpheres> {
        launch(useCase.spheres)

        database.feedPollingStateQueries.actorSetFailure(END_TIME)

        busyWait {
            val value = values.lastOrNull()
            assertEquals(value?.selectedSphereId, null)
            assertEquals(value?.spheres, emptyList())
            assertEquals(value?.status, SphereSelector.Status.Failed)
        }
    }

    @Test
    fun successButEmpty() = flowTest<SphereSelector.AllSpheres> {
        launch(useCase.spheres)

        database.feedPollingStateQueries.actorSetSuccess(END_TIME)

        busyWait {
            val value = values.lastOrNull()
            assertEquals(value?.selectedSphereId, null)
            assertEquals(value?.spheres, emptyList())
            assertEquals(value?.status, SphereSelector.Status.Success)
        }
    }

    @Test
    fun successAndPopulated() = flowTest<SphereSelector.AllSpheres> {
        val stubSpheres = SphereStub.spheres
        val expectedResult = stubSpheres
            .map(SphereStub::convertToQueryResult)
            .map(GetAllSpheres::toFeedStateSphere)

        launch(useCase.spheres)
        setLastFetchSuccessScenario(stubSpheres)

        database.transaction {
            stubSpheres.forEach(database.sphereQueries::upsert)
            database.feedPollingStateQueries.actorSetSuccess(END_TIME)
        }

        busyWait {
            val value = values.lastOrNull()
            assertEquals(value?.selectedSphereId, stubSpheres[0].id)
            assertEquals(value?.spheres, expectedResult)
            assertEquals(value?.status, SphereSelector.Status.Success)
        }
    }

    @Test
    fun selectSphere() = flowTest<SphereSelector.AllSpheres> {
        val stubSpheres = SphereStub.spheres
        setLastFetchSuccessScenario(stubSpheres)

        launch(useCase.spheres)

        busyWait {
            assertEquals(values.firstOrNull()?.selectedSphereId, stubSpheres[0].id)
        }

        useCase.selectSphere(stubSpheres[1].id)

        busyWait {
            assertEquals(values.lastOrNull()?.selectedSphereId, stubSpheres[1].id)
        }
    }

    private fun setLastFetchSuccessScenario(spheres: List<Sphere>) {
        require(spheres.isNotEmpty())

        database.transaction {
            SphereStub.spheres.forEach(database.sphereQueries::upsert)
            database.feedPollingStateQueries.actorSetSuccess(END_TIME)
            database.feedPollingStateQueries.setSelectedSphere(spheres[0].id)
        }
    }
}
