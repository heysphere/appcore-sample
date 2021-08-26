package me.sphere.appcore.chat

import app.cash.turbine.test
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import me.sphere.appcore.chat.ConversationGateway.AnchorResult
import me.sphere.appcore.chat.PaginationMachine.AnchorResolutionStatus
import me.sphere.appcore.chat.PaginationMachine.EdgeStatus
import me.sphere.appcore.chat.PaginationMachine.StreamingStatus
import me.sphere.appcore.stubs.ConversationEventStub as Stub
import me.sphere.appcore.utils.freeze
import me.sphere.sqldelight.operations.chat.FetchConversationEventsOperation
import me.sphere.sqldelight.operations.chat.SelectAnchorConversationEventOperation
import me.sphere.test.LoopTests
import me.sphere.appcore.utils.Atomic
import me.sphere.models.ConversationId
import me.sphere.models.chat.*
import me.sphere.models.chat.ConversationAnchorRationale.*
import me.sphere.test.support.TestLogger
import me.sphere.test.support.setFrozen
import me.sphere.sqldelight.chat.ConversationEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime

internal class PaginationMachineTests : LoopTests<PaginationMachine, PaginationMachine.State, PaginationMachine.Event, ConversationEnvironment>(PaginationMachine) {
    private val gateway = StubConversationGateway()
    private val testException = object: Throwable() { init { freeze() } }

    override val environment = ConversationEnvironment(
        conversationId = ConversationId("stub"),
        filter = ConversationEventFilter.None,
        gateway = gateway,
        logger = TestLogger
    )

    @Test
    fun initial_state() {
        gateway.stubSelectAnchor.setFrozen { suspendForever() }

        initialize(state = initial())

        busyWait {
            assertEquals(state.first(), initial())
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun should_not_start_resolving_anchor_until_viewer_has_read_permission() = initial().runTesting {
        gateway.stubSelectAnchor.setFrozen { suspendForever() }

        state.test {
            assertEquals(initial(), actual = expectItem())

            send(PaginationMachine.Event.DidConfirmViewerReadPermission)
            assertEquals(
                initial().copy(
                    anchorResolutionStatus = AnchorResolutionStatus
                        .Resolving(ConversationLandingTarget.Resume)
                ),
                actual = expectItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun resolve_anchor_successfully_nonEmptyConversation() {
        val stubIndex = ConversationEventIndex(Instant.fromEpochSeconds(128, 512))
        val stubEvent = Stub.textMessage().copy(createdAt = stubIndex.createdAt)
        val stubAnchorId = stubEvent.itemId()
        val operationInput = Atomic<SelectAnchorConversationEventOperation.Input?>(null)

        val hasStartedStreaming = Atomic(false)

        gateway.stubSelectAnchor.setFrozen {
            operationInput.value = it
            AnchorResult(stubEvent, null, LastOrEmpty)
        }
        gateway.stubFetchEvents.setFrozen { neverFlow() }
        gateway.stubStreamAppended.setFrozen {
            neverFlow<List<ConversationEvent>>()
                .onStart { hasStartedStreaming.value = true }
        }

        val status = AnchorResolutionStatus.Resolving(ConversationLandingTarget.Resume)
        val initial = initial().copy(anchorResolutionStatus = status)
        initialize(state = initial)

        busyWait {
            assertEquals(
                initial.copy(
                    container = ConversationEventsContainer(listOf(stubEvent)),
                    anchorResolutionStatus = AnchorResolutionStatus.Resolved(stubAnchorId, null, LastOrEmpty),
                    backward = EdgeStatus.Fetching,
                    forward = EdgeStatus.Fetched.EndOfCollection,
                    streamingStatus = StreamingStatus.Active(stubIndex)
                ),
                state.last()
            )
            assertEquals(ConversationLandingTarget.Resume, operationInput.value?.landingTarget)
        }

        busyWait {
            assertTrue(hasStartedStreaming.value)
        }
    }

    @Test
    fun resolve_anchor_successfully_knownEmptyConversation() {
        val operationInput = Atomic<SelectAnchorConversationEventOperation.Input?>(null)
        val hasStartedStreaming = Atomic(false)

        gateway.stubSelectAnchor.setFrozen {
            operationInput.value = it
            AnchorResult(null, null, LastOrEmpty)
        }
        gateway.stubFetchEvents.setFrozen { neverFlow() }
        gateway.stubStreamAppended.setFrozen {
            neverFlow<List<ConversationEvent>>()
                .onStart { hasStartedStreaming.value = true }
        }

        val initial = initial().copy(
            anchorResolutionStatus = AnchorResolutionStatus
                .Resolving(ConversationLandingTarget.Resume)
        )
        initialize(state = initial)

        busyWait {
            assertEquals(
                initial.copy(
                    container = ConversationEventsContainer(),
                    anchorResolutionStatus = AnchorResolutionStatus.Resolved(null, null, LastOrEmpty),
                    backward = EdgeStatus.Fetched.EndOfCollection,
                    forward = EdgeStatus.Fetched.EndOfCollection,
                    initialFetchDone = true,
                    streamingStatus = StreamingStatus.Active(null)
                ),
                state.last()
            )
            assertEquals(ConversationLandingTarget.Resume, operationInput.value?.landingTarget)
        }

        busyWait {
            assertTrue(hasStartedStreaming.value)
        }
    }

    @Test
    fun resolve_anchor_failed() {
        gateway.stubSelectAnchor.setFrozen { throw testException }

        val landingTarget = ConversationLandingTarget.Resume
        val status = AnchorResolutionStatus.Resolving(landingTarget)
        val initial = initial().copy(anchorResolutionStatus = status)
        initialize(state = initial)

        busyWait {
            assertEquals(
                initial.copy(anchorResolutionStatus = AnchorResolutionStatus.Failed(landingTarget)),
                state.last()
            )
        }
    }

    @Test
    fun paginate_forward_successfully() {
        paginate_successfully(
            direction = PaginationDirection.Forward,
            isEndOfCollection = false
        )
    }

    @Test
    fun paginate_backward_successfully() {
        paginate_successfully(
            direction = PaginationDirection.Backward,
            isEndOfCollection = false
        )
    }

    @Test
    fun paginate_forward_to_end_of_collection_successfully() {
        val streamingStarted = Atomic(false)
        gateway.stubStreamAppended.set { _ ->
            neverFlow<List<ConversationEvent>>()
                .onStart { streamingStarted.value = true }
        }

        paginate_successfully(
            direction = PaginationDirection.Forward,
            isEndOfCollection = true
        )

        busyWait {
            assertTrue(streamingStarted.value)
        }
    }

    @Test
    fun paginate_backward_to_end_of_collection_successfully() {
        paginate_successfully(
            direction = PaginationDirection.Backward,
            isEndOfCollection = true
        )
    }

    private fun paginate_successfully(direction: PaginationDirection, isEndOfCollection: Boolean) {
        val allEvents = (0..10).map {
            Stub.textMessage().copy(
                id = LocalConversationEventId(it.toLong()),
                eventId = RemoteConversationEventId("$it")
            )
        }
        val anchorEvent = allEvents[0]
        val stubAnchorId = anchorEvent.itemId()
        val nextPage = allEvents.subList(1, 10)

        val oppositeEdge = when (direction) {
            PaginationDirection.Backward -> PaginationDirection.Forward
            PaginationDirection.Forward -> PaginationDirection.Backward
        }

        val initialState = initial()
            .copy(
                container = ConversationEventsContainer(listOf(anchorEvent)),
                anchorResolutionStatus = AnchorResolutionStatus.Resolved(stubAnchorId, null, LastOrEmpty),
            )
            .withEdgeStatus(EdgeStatus.Fetching, direction)
            .withEdgeStatus(EdgeStatus.Fetched.HasMore, oppositeEdge)

        val operationInput = Atomic<FetchConversationEventsOperation.Input?>(null)
        
        gateway.stubFetchEvents.setFrozen {
            operationInput.value = it
            flowOf(ConversationGateway.FetchResult(page = nextPage, isTail = isEndOfCollection))
        }

        initialize(state = initialState)

        val expectedNewEdgeStatus = when (isEndOfCollection) {
            true -> EdgeStatus.Fetched.EndOfCollection
            false -> EdgeStatus.Fetched.HasMore
        }

        busyWait {
            assertEquals(
                state.last(),
                initialState
                    .withEdgeStatus(expectedNewEdgeStatus, direction)
                    .copy(
                        container = when (direction) {
                            PaginationDirection.Forward ->
                                ConversationEventsContainer(listOf(anchorEvent) + nextPage)
                            PaginationDirection.Backward ->
                                ConversationEventsContainer(nextPage + listOf(anchorEvent))
                        },
                        initialFetchDone = true,
                        streamingStatus = when (isEndOfCollection && direction == PaginationDirection.Forward) {
                            true -> StreamingStatus.Active(
                                nextPage.last().createdAt.let(::ConversationEventIndex)
                            )
                            false -> StreamingStatus.Inactive
                        }
                    ),
            )
            assertEquals(direction, operationInput.value?.direction)
        }
    }

    @Test
    fun paginate_forward_failed() {
        paginate_failed(direction = PaginationDirection.Forward)
    }

    @Test
    fun paginate_backward_failed() {
        paginate_failed(direction = PaginationDirection.Backward)
    }

    private fun paginate_failed(direction: PaginationDirection) {
        val anchorEvent = Stub.textMessage()
        val stubAnchorId = anchorEvent.itemId()

        val initialState = initial()
            .copy(
                container = ConversationEventsContainer(listOf(anchorEvent)),
                anchorResolutionStatus = AnchorResolutionStatus.Resolved(stubAnchorId, null, LastOrEmpty)
            )
            .withEdgeStatus(EdgeStatus.Fetching, direction)

        val operationInput = Atomic<FetchConversationEventsOperation.Input?>(null)
        gateway.stubFetchEvents.setFrozen {
            operationInput.value = it
            flow { throw testException }
        }

        initialize(state = initialState)

        busyWait {
            assertEquals(
                initialState.withEdgeStatus(EdgeStatus.Failed, direction),
                state.last()
            )
            assertEquals(direction, operationInput.value?.direction)
        }
    }

    private fun initial() = PaginationMachine.State(
        container = ConversationEventsContainer(),
        anchorResolutionStatus = AnchorResolutionStatus.AwaitingReadPermissionCheck(
            ConversationLandingTarget.Resume
        ),
        backward = EdgeStatus.Uninitialized,
        forward = EdgeStatus.Uninitialized,
        initialFetchDone = false,
        streamingStatus = StreamingStatus.Inactive
    )
}
