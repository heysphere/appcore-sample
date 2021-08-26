package me.sphere.graphql

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import me.sphere.appcore.utils.Atomic
import me.sphere.graphql.event.SphereEventFetchingActor
import me.sphere.models.FeedPollingStatus
import me.sphere.network.HTTPRequest
import me.sphere.network.HTTPResponse
import me.sphere.network.HTTPStatusCode
import me.sphere.test.DbTests
import me.sphere.test.StubHTTPClient
import me.sphere.sqldelight.storeActor.SphereEventFetchingState
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class SphereEventFetchingActorTests: DbTests() {
    val httpClient = StubHTTPClient()
    val actor = SphereEventFetchingActor(httpClient, database, StoreScope)

    @BeforeTest
    fun test_preconditions() {
        database.sphereQueries.upsert(Fixtures.sphere)
        actor.attach()
    }

    @Test
    fun should_insert_event_and_mark_fetching_as_success() {
        val httpRequest = Atomic<HTTPRequest<String>?>(null)

        httpClient.stubRequest.value = { request ->
            httpRequest.value = request
            flowOf(HTTPResponse(HTTPStatusCode(200), Fixtures.getSphereEventResponse))
        }

        insertFetchingStateIfNotExist()

        busyWait {
            val state = getFetchingState()
            assertEquals(state.status, FeedPollingStatus.Success)
            assertTrue(state.lastUpdated != null)
            assertTrue(httpRequest.value != null)
        }

        busyWait {
            val event = database.sphereEventQueries.getById(Fixtures.sphereEvent.id).executeAsOne()
            assertEquals(Fixtures.sphereEvent, event)
        }
    }

    @Test
    fun should_refetch_when_requested() {
        val httpRequest = Atomic<HTTPRequest<String>?>(null)

        httpClient.stubRequest.value = { request ->
            httpRequest.value = request
            neverFlow()
        }

        insertFetchingStateIfNotExist(FeedPollingStatus.Failure)

        busyWait {
            assertEquals(getFetchingState().status, FeedPollingStatus.Failure)
            assertTrue(httpRequest.value == null)
        }

        database.sphereEventFetchingStateQueries.fetchNow(Fixtures.sphereEvent.id)

        busyWait {
            val state = getFetchingState()
            assertEquals(state.status, FeedPollingStatus.Loading)
            assertTrue(httpRequest.value != null)
        }
    }

    @Test
    fun should_mark_fetching_as_failed() {
        val httpRequest = Atomic<HTTPRequest<String>?>(null)

        httpClient.stubRequest.value = { request ->
            httpRequest.value = request
            flow { throw Throwable("Test exception") }
        }

        insertFetchingStateIfNotExist()

        busyWait {
            val state = getFetchingState()
            assertEquals(state.status, FeedPollingStatus.Failure)
            assertTrue(state.lastUpdated == null)
            assertTrue(httpRequest.value != null)
        }
    }

    private fun getFetchingState() = database.sphereEventFetchingStateQueries
        .get(Fixtures.sphere.id, Fixtures.sphereEvent.id)
        .executeAsOne()

    private fun insertFetchingStateIfNotExist(status: FeedPollingStatus = FeedPollingStatus.Loading) {
        database.sphereEventFetchingStateQueries.insertIfNotExist(
            SphereEventFetchingState(
                eventId = Fixtures.sphereEvent.id, sphereId = Fixtures.sphere.id, status = status, lastUpdated = null
            )
        )
    }
}
