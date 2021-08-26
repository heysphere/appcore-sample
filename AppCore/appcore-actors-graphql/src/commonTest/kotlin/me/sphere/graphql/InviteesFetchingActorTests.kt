package me.sphere.graphql

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import me.sphere.appcore.utils.Atomic
import me.sphere.models.FeedPollingStatus
import me.sphere.models.event.SphereEventRSVP
import me.sphere.network.HTTPRequest
import me.sphere.network.HTTPResponse
import me.sphere.network.HTTPStatusCode
import me.sphere.test.DbTests
import me.sphere.test.StubHTTPClient
import me.sphere.sqldelight.storeActor.InviteesFetchingState
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class InviteesFetchingActorTests: DbTests() {
    val httpClient = StubHTTPClient()
    val actor = InviteesFetchingActor(httpClient, database, StoreScope)

    @BeforeTest
    fun test_preconditions() {
        database.sphereQueries.upsert(Fixtures.sphere)
        database.sphereEventQueries.upsert(Fixtures.sphereEvent)
        actor.attach()
    }

    @Test
    fun should_insert_event_and_mark_fetching_as_success() {
        val httpRequest = Atomic<HTTPRequest<String>?>(null)

        httpClient.stubRequest.value = { request ->
            httpRequest.value = request
            flowOf(HTTPResponse(HTTPStatusCode(200), Fixtures.inviteesResponse))
        }

        insertFetchingStateIfNotExist()

        busyWait {
            val state = getFetchingState()
            assertEquals(state.status, FeedPollingStatus.Success)
            assertTrue(state.lastUpdated != null)
            assertTrue(httpRequest.value != null)
        }

        busyWait {
            val goingInvitees = database.sphereEventInviteeQueries.get(Fixtures.sphereEvent.id, SphereEventRSVP.Going).executeAsList()
            assertEquals(
                Fixtures.goingInvitees.map { it.id }.toSet(),
                goingInvitees.map { it.id }.toSet()
            )
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

        database.inviteesFetchingStateQueries.fetchNow(Fixtures.sphereEvent.id)

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

    private fun getFetchingState() = database.inviteesFetchingStateQueries
        .get(Fixtures.sphereEvent.id)
        .executeAsOne()

    private fun insertFetchingStateIfNotExist(status: FeedPollingStatus = FeedPollingStatus.Loading) {
        database.inviteesFetchingStateQueries.insertIfNotExist(
            InviteesFetchingState(
                eventId = Fixtures.sphereEvent.id, status = status, lastUpdated = null
            )
        )
    }
}
