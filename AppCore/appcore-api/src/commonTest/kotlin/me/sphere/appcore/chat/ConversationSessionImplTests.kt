package me.sphere.appcore.chat

import kotlinx.datetime.Instant
import me.sphere.appcore.stubs.ConversationEventStub as Stub
import me.sphere.appcore.usecases.chat.Conversation
import me.sphere.appcore.utils.APPCORE_INSTANT_MAX
import me.sphere.models.ConversationId
import me.sphere.models.OutgoingMessageId
import me.sphere.models.SphereId
import me.sphere.models.chat.*
import me.sphere.sqldelight.chat.OutgoingMessage
import kotlin.test.Test
import kotlin.test.assertEquals

class ConversationSessionImplTests {
    private val outgoingId = OutgoingMessageId("client-gen-01")
    private val stubEpochZero = Instant.fromEpochSeconds(0, 0)
    private val stubEvent = Stub.textMessage(clientGeneratedId = outgoingId.rawValue, createdAt = stubEpochZero)
    private val stubOutgoingMessage = OutgoingMessage(
        outgoingId,
        ConversationId(""),
        APPCORE_INSTANT_MAX,
        TextMessageInput("hello outgoing", emptyList()),
        important = false,
        null,
        OutgoingMessageStatus.Sending,
        null,
        null
    )

    private val committedVersion = stubItem(
        Conversation.ItemId.Committed.Message("stub-0", outgoingId.rawValue),
        stubEpochZero,
        Conversation.Message.Status.OK,
        "Stub event 0",
        Stub.sender,
        canBeDeleted = false
    )

    private val outgoingVersion = stubItem(
        Conversation.ItemId.OutgoingMessage(outgoingId.rawValue),
        stubOutgoingMessage.createdAt,
        Conversation.Message.Status.SENDING,
        "hello outgoing",
        stubViewerState.info.info,
        canBeDeleted = true
    )

    @Test
    fun mapToConversationItems_prefers_committed_over_outgoing_if_there_is_a_conflicting_clientGeneratedId() {
        val state = ConversationSessionImpl.State(
            container = ConversationEventsContainer(listOf(stubEvent)),
            outgoingMessages = listOf(stubOutgoingMessage),
            viewerState = stubViewerState,
            anchorResolutionStatus = PaginationMachine.AnchorResolutionStatus.Resolving(ConversationLandingTarget.Resume),
            initialFetchDone = true
        )

        assertEquals(listOf(committedVersion), actual = state.mapToConversationItems())
    }

    @Test
    fun mapToConversationItems_does_not_output_any_outgoing_message_since_forward_pagination_has_not_reached_the_end() {
        val state = ConversationSessionImpl.State(
            container = ConversationEventsContainer(emptyList()),
            outgoingMessages = listOf(stubOutgoingMessage),
            viewerState = stubViewerState,
            anchorResolutionStatus = PaginationMachine.AnchorResolutionStatus.Resolving(ConversationLandingTarget.Resume),
            forward = PaginationMachine.EdgeStatus.Fetched.HasMore,
            initialFetchDone = true
        )

        assertEquals(emptyList(), actual = state.mapToConversationItems())
    }

    @Test
    fun mapToConversationItems_does_not_output_any_item_because_initialFetchDone_is_false() {
        val state = ConversationSessionImpl.State(
            container = ConversationEventsContainer(listOf(stubEvent)),
            outgoingMessages = listOf(stubOutgoingMessage),
            viewerState = stubViewerState,
            anchorResolutionStatus = PaginationMachine.AnchorResolutionStatus.Resolved(null, null, ConversationAnchorRationale.LastOrEmpty),
            initialFetchDone = false
        )

        assertEquals(emptyList(), actual = state.mapToConversationItems())
    }

    @Test
    fun mapToConversationItems_does_not_output_any_item_because_ViewerMachineState_is_not_OK() {
        val state = ConversationSessionImpl.State(
            container = ConversationEventsContainer(listOf(stubEvent)),
            outgoingMessages = listOf(stubOutgoingMessage),
            viewerState = ViewerMachine.State.Loading,
            anchorResolutionStatus = PaginationMachine.AnchorResolutionStatus.Resolved(null, null, ConversationAnchorRationale.LastOrEmpty),
            initialFetchDone = false
        )

        assertEquals(emptyList(), actual = state.mapToConversationItems())
    }

    @Test
    fun mapToConversationItems_outputs_an_item_from_a_outgoing_message() {
        val state = ConversationSessionImpl.State(
            container = ConversationEventsContainer(emptyList()),
            outgoingMessages = listOf(stubOutgoingMessage),
            viewerState = stubViewerState,
            anchorResolutionStatus = PaginationMachine.AnchorResolutionStatus.Resolving(ConversationLandingTarget.Resume),
            forward = PaginationMachine.EdgeStatus.Fetched.EndOfCollection,
            initialFetchDone = true
        )

        assertEquals(listOf(outgoingVersion), actual = state.mapToConversationItems())
    }

    @Test
    fun mapToConversationItems_outputs_an_item_from_a_committed_message() {
        val state = ConversationSessionImpl.State(
            container = ConversationEventsContainer(listOf(stubEvent)),
            outgoingMessages = listOf(),
            viewerState = stubViewerState,
            anchorResolutionStatus = PaginationMachine.AnchorResolutionStatus.Resolving(ConversationLandingTarget.Resume),
            initialFetchDone = true
        )

        assertEquals(listOf(committedVersion), actual = state.mapToConversationItems())
    }
}

private val stubViewerState = ViewerMachine.State.OK(
    Conversation.Context(SphereId("stub"), ConversationId("stub")),
    ViewerInfo(
        info = Stub.sender,
        isFollowing = false,
        capabilities = ChatCapabilities(),
        conversationType = ConversationType.Channel,
        unreadCount = 0,
        lastSeenMessageId = null
    )
)

private fun stubItem(
    id: Conversation.ItemId,
    createdAt: Instant,
    status: Conversation.Message.Status,
    body: String,
    sender: MessageSenderInfo,
    canBeDeleted: Boolean
) = Conversation.Item.TextMessage(
    textContent = TextContent(body, emptyList()),
    id = id,
    created = createdAt,
    status = status,
    senderInfo = sender,
    reactions = emptyList(),
    isPinned = false,
    shareSource = null,
    pinShareTarget = null,
    linkPreviews = emptyList(),
    replyToSourceInfo = null,
    capabilities = Conversation.PrimitiveMessage.Capabilities(
        canDelete = canBeDeleted,
        canReactTo = false,
        canPinOrUnpin = false,
        canReply = false,
        canRemoveAppreciation = false,
        canAddAppreciation = false
    )
)
