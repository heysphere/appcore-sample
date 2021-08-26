@file:UseSerializers(InstantFirestoreSerializer::class)

package me.sphere.appcore.firestore

import kotlinx.datetime.Instant
import kotlinx.serialization.*

@Serializable
data class FirestoreConversation(
    val state: ConversationState,
    val mentionables: Map<String, MentionableInfo>? = null,
    val lastUpdatedMessageInfo: LastUpdatedMessageInfo? = null
) {
    @Serializable
    enum class ConversationState {
        @SerialName("live")
        Live,
        @SerialName("ended")
        Ended;
    }

    @Serializable
    data class MentionableInfo(
        val name: String,
        val image: String? = null,
        val color: String? = null
    )

    @Serializable
    data class LastUpdatedMessageInfo(
        val messageId: String,
        val updatedAt: Instant
    )
}
