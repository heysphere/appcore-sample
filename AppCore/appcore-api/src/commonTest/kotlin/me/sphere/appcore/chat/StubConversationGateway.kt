@file:Suppress("MemberVisibilityCanBePrivate")
package me.sphere.appcore.chat

import com.squareup.sqldelight.internal.Atomic
import kotlinx.coroutines.flow.*
import me.sphere.appcore.usecases.chat.Conversation
import me.sphere.appcore.utils.freeze
import me.sphere.appcore.utils.frozenLambda
import me.sphere.appcore.utils.frozenSuspend
import me.sphere.models.ConversationId
import me.sphere.models.chat.*
import me.sphere.sqldelight.operations.chat.StreamAppendedConversationEventsOperation
import me.sphere.sqldelight.chat.*
import me.sphere.sqldelight.operations.chat.FetchConversationEventsOperation
import me.sphere.sqldelight.operations.chat.SelectAnchorConversationEventOperation

@Suppress("MemberVisibilityCanBePrivate")
internal class StubConversationGateway: ConversationGateway {
    val stubSelectAnchor: Atomic<suspend (SelectAnchorConversationEventOperation.Input) -> ConversationGateway.AnchorResult>
        = Atomic(frozenSuspend { _ -> TODO("Stub is called unexpectedly") })

    val stubFetchEvents: Atomic<(FetchConversationEventsOperation.Input) -> Flow<ConversationGateway.FetchResult>>
        = Atomic(frozenLambda { _ -> TODO("Stub is called unexpectedly") })

    val stubStreamAppended: Atomic<(StreamAppendedConversationEventsOperation.Input) -> Flow<List<ConversationEvent>>>
        = Atomic(frozenLambda { _ -> TODO("Stub is called unexpectedly") })

    val stubStreamOutgoing: Atomic<(ConversationId) -> Flow<List<OutgoingMessage>>>
        = Atomic(frozenLambda { _ -> TODO("Stub is called unexpectedly") })

    val stubStreamHardDeleted: Atomic<(ConversationId) -> Flow<List<RemoteConversationEventId>>>
        = Atomic(frozenLambda { _ -> TODO("Stub is called unexpectedly")  })

    val stubMonitorUpdates: Atomic<(ConversationId) -> Flow<RemoteUpdateMark>>
        = Atomic(frozenLambda { _ -> TODO("Stub is called unexpectedly") })

    val stubMonitorLocalUpdates: Atomic<(ConversationId) -> Flow<LocalUpdateMark>>
        = Atomic(frozenLambda { _ -> TODO("Stub is called unexpectedly") })

    val stubFetchUpdatedMessages: Atomic<suspend (RemoteUpdateMark, ConversationId, ClosedRange<ConversationEventIndex>) -> ConversationGateway.UpdateFetchResult<RemoteUpdateMark>>
        = Atomic(frozenSuspend { _, _, _ -> TODO("Stub is called unexpectedly") })

    val stubFetchLocallyUpdatedMessages: Atomic<(LocalUpdateMark, ConversationId, ClosedRange<ConversationEventIndex>) -> ConversationGateway.UpdateFetchResult<LocalUpdateMark>>
        = Atomic(frozenLambda { _, _, _ -> TODO("Stub is called unexpectedly") })

    val stubEnsureViewerCanAccessConversation: Atomic<suspend (ConversationId) -> Pair<Conversation.Context, ViewerInfo>>
        = Atomic(frozenSuspend { _ -> TODO("Stub is called unexpectedly") })

    val stubStreamViewerInfo: Atomic<(ConversationId) -> Flow<ViewerInfo>>
        = Atomic(frozenLambda { _ -> TODO("Stub is called unexpectedly") })

    init { freeze() }

    override suspend fun selectAnchor(input: SelectAnchorConversationEventOperation.Input): ConversationGateway.AnchorResult
        = stubSelectAnchor.get().invoke(input)

    override fun fetchEvents(input: FetchConversationEventsOperation.Input): Flow<ConversationGateway.FetchResult>
        = stubFetchEvents.get().invoke(input)

    override fun streamAppended(input: StreamAppendedConversationEventsOperation.Input): Flow<List<ConversationEvent>>
        = stubStreamAppended.get().invoke(input)

    override fun streamOutgoing(id: ConversationId, filter: ConversationEventFilter): Flow<List<OutgoingMessage>>
        = stubStreamOutgoing.get().invoke(id)

    override fun streamHardDeleted(id: ConversationId): Flow<List<RemoteConversationEventId>>
        = stubStreamHardDeleted.get().invoke(id)

    override fun monitorUpdates(conversationId: ConversationId): Flow<RemoteUpdateMark>
        = stubMonitorUpdates.get().invoke(conversationId)

    override fun monitorLocalUpdates(conversationId: ConversationId): Flow<LocalUpdateMark>
        = stubMonitorLocalUpdates.get().invoke(conversationId)

    override suspend fun ensureViewerCanAccessConversation(id: ConversationId): Pair<Conversation.Context, ViewerInfo>
        = stubEnsureViewerCanAccessConversation.get().invoke(id)

    override fun streamViewerInfo(id: ConversationId): Flow<ViewerInfo>
        = stubStreamViewerInfo.get().invoke(id)

    override suspend fun fetchUpdatedMessages(
        previousMark: RemoteUpdateMark,
        range: ClosedRange<ConversationEventIndex>,
        conversationId: ConversationId,
        filter: ConversationEventFilter
    ): ConversationGateway.UpdateFetchResult<RemoteUpdateMark>
        = stubFetchUpdatedMessages.get().invoke(previousMark, conversationId, range)

    override fun fetchLocallyUpdatedMessages(
        previousMark: LocalUpdateMark,
        range: ClosedRange<ConversationEventIndex>,
        conversationId: ConversationId,
        filter: ConversationEventFilter
    ): ConversationGateway.UpdateFetchResult<LocalUpdateMark>
        = stubFetchLocallyUpdatedMessages.get().invoke(previousMark, conversationId, range)
}
