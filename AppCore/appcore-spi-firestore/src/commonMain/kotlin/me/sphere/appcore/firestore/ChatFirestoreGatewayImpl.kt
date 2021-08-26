package me.sphere.appcore.firestore

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import me.sphere.appcore.firestore.ChatFirestoreGatewayImpl.Companion.IMPORTANT
import me.sphere.appcore.firestore.ChatFirestoreGatewayImpl.Companion.REMOVED
import me.sphere.appcore.utils.*

typealias FirestoreGatewayImpl = ChatFirestoreGatewayImpl

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class ChatFirestoreGatewayImpl : AbstractFirestoreGateway(), ChatFirestoreGateway {
    companion object {
        const val CONVERSATION = "conversations"
        const val ATTENDANT = "attendants"
        const val MESSAGE = "messages"

        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"

        const val IMPORTANT = "important"
        const val REMOVED = "removed"
    }

    init { freeze() }

    override fun observeLastMessages(conversationId: String, limit: Int, filter: FirestoreMessageFilter, start: Instant?) = flatMapLatestFirestore { store ->
        store
            .messagesByCreatedAtDescQuery(conversationId)
            .apply(filter)
            .apply { start?.let(this::endAt) ?: this }
            .limit(limit)
            .listen(FirestoreMessage.serializer())
            .map { it.reversed() }
    }

    override suspend fun lastMessage(conversationId: String, filter: FirestoreMessageFilter) = firestoreOrThrow().run {
        messagesByCreatedAtDescQuery(conversationId)
            .apply(filter)
            .limit(1)
            .get(FirestoreMessage.serializer())
            .single()
            .firstOrNull()
    }

    override suspend fun message(conversationId: String, messageId: String) = firestoreOrThrow().run {
        val flow = collection(CONVERSATION)
            .document(conversationId)
            .collection(MESSAGE)
            .document(messageId)
            .get(FirestoreMessage.serializer())

        runCatching { flow.single() }
            .recoverCatching { if (it is DocumentDoesNotExistException) throw it else null }
            .getOrThrow()
    }

    override fun getMessagesBefore(createdAt: Instant, conversationId: String, limit: Int, filter: FirestoreMessageFilter) = flatMapOnceFirestore { firestore ->
        firestore
            .messagesByCreatedAtDescQuery(conversationId)
            .apply(filter)
            .startAfter(createdAt)
            .limit(first = limit)
            .get(FirestoreMessage.serializer())
            .map { it.reversed() }
    }

    override fun getMessagesAfter(createdAt: Instant, conversationId: String, limit: Int, filter: FirestoreMessageFilter) = flatMapOnceFirestore { firestore ->
        firestore
            .messagesByCreatedAtDescQuery(conversationId)
            .apply(filter)
            .endBefore(createdAt)
            .limitLast(count = limit)
            .get(FirestoreMessage.serializer())
            .map { it.reversed() }
    }

    override fun getMessagesInRange(startExclusive: Instant, end: Instant, conversationId: String, filter: FirestoreMessageFilter) = flatMapOnceFirestore { firestore ->
        firestore
            .messagesByCreatedAtDescQuery(conversationId)
            .apply(filter)
            .endBefore(startExclusive)
            .startAfter(end)
            .get(FirestoreMessage.serializer())
            .map { it.reversed() }
    }

    override suspend fun updatedMessages(startExclusive: Instant, limit: Int?, conversationId: String, filter: FirestoreMessageFilter) = firestoreOrThrow()
        .collection(CONVERSATION)
        .document(conversationId)
        .collection(MESSAGE)
        .orderBy(UPDATED_AT, CollectionQuery.OrderByDirection.Ascending)
        .apply(filter)
        .startAfter(startExclusive)
        .apply { if (limit != null) limit(limit) }
        .get(FirestoreMessage.serializer())
        .single()

    override fun observeAttendant(attendantId: String, conversationId: String): Flow<FirestoreAttendant>
        = flatMapLatestFirestore {
            it
                .collection(CONVERSATION)
                .document(conversationId)
                .collection(ATTENDANT)
                .document(attendantId)
                .listen(FirestoreAttendant.serializer())
        }

    override fun attendant(attendantId: String, conversationId: String): Flow<FirestoreAttendant>
        = flatMapOnceFirestore {
            autoreleasepool {
                it
                    .collection(CONVERSATION)
                    .document(conversationId)
                    .collection(ATTENDANT)
                    .document(attendantId)
                    .get(FirestoreAttendant.serializer())
            }
        }

    override fun observeConversation(conversationId: String) = flatMapLatestFirestore {
        it
            .collection(CONVERSATION)
            .document(conversationId)
            .listen(FirestoreConversation.serializer())
    }

    private fun Firestore.messagesByCreatedAtDescQuery(conversationId: String) = collection(CONVERSATION)
        .document(conversationId)
        .collection(MESSAGE)
        .orderBy(CREATED_AT, CollectionQuery.OrderByDirection.Descending)
}

private fun CollectionQuery.apply(filter: FirestoreMessageFilter): CollectionQuery = this
    .run { if (filter.onlyImportant) whereEqualTo(IMPORTANT, true) else this }
    .run { if (filter.onlyNotRemoved) whereEqualTo(REMOVED, false) else this }
