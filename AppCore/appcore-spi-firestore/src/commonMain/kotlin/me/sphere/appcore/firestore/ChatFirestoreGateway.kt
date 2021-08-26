package me.sphere.appcore.firestore

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

interface ChatFirestoreGateway {
    /**
     * @param start The inclusive start in message creation time (`createdAt`) to listen for.
     */
    fun observeLastMessages(conversationId: String, limit: Int, filter: FirestoreMessageFilter, start: Instant? = null): Flow<List<FirestoreMessage>>
    suspend fun lastMessage(conversationId: String, filter: FirestoreMessageFilter): FirestoreMessage?
    suspend fun message(conversationId: String, messageId: String): FirestoreMessage?
    fun getMessagesBefore(createdAt: Instant, conversationId: String, limit: Int, filter: FirestoreMessageFilter): Flow<List<FirestoreMessage>>
    fun getMessagesAfter(createdAt: Instant, conversationId: String, limit: Int, filter: FirestoreMessageFilter): Flow<List<FirestoreMessage>>
    fun getMessagesInRange(startExclusive: Instant, end: Instant, conversationId: String, filter: FirestoreMessageFilter): Flow<List<FirestoreMessage>>

    /**
     * Fetch updated messages, in the chronological order of when they were last updated.
     *
     * @param startExclusive The exclusive start in message update time (`updatedAt`) to start searching from. This is
     * usually the `updatedAt` timestamp of the last updated message having been processed locally.
     */
    suspend fun updatedMessages(startExclusive: Instant, limit: Int?, conversationId: String, filter: FirestoreMessageFilter): List<FirestoreMessage>

    fun observeAttendant(attendantId: String, conversationId: String): Flow<FirestoreAttendant>
    fun attendant(attendantId: String, conversationId: String): Flow<FirestoreAttendant>
    fun observeConversation(conversationId: String): Flow<FirestoreConversation>
}
