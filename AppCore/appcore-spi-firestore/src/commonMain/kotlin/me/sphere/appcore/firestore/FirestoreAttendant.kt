@file:UseSerializers(InstantFirestoreSerializer::class)

package me.sphere.appcore.firestore

import kotlinx.datetime.Instant
import kotlinx.serialization.*

@Serializable
data class FirestoreAttendant(
    val conversationId: String,
    val displayName: String,
    val displayColor: String,
    val displayImageFilename: String? = null,
    val mutedConversation: Boolean,
    val outstandingImportantMessages: List<String>,
    val followsConversation: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant? = null,
    val role: Role
) {

    @Serializable
    enum class Role {
        @SerialName("conversation_creator")
        Creator,

        @SerialName("conversation_participant")
        Participant;
    }
}
