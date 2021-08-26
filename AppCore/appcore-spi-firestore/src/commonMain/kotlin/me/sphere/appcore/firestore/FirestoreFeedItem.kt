@file:UseSerializers(InstantFirestoreSerializer::class)
package me.sphere.appcore.firestore

import kotlinx.datetime.Instant
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure

@Serializable(with = FirestoreFeedItem.Serializer::class)
sealed class FirestoreFeedItem {
    abstract val id: String
    abstract val createdAt: Instant
    abstract val updatedAt: Instant?
    abstract val prioritized: Boolean
    abstract val lastActivityAt: Instant
    abstract val expiresAt: Instant?
    abstract val archived: Boolean

    @OptIn(ExperimentalSerializationApi::class)
    object Serializer: KSerializer<FirestoreFeedItem> {
        @Serializable
        enum class ItemType {
            SPHERE_CHAT, SPHERE_EVENT
        }

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("FeedItem") {
            element("type", ItemType.serializer().descriptor)
        }

        override fun deserialize(decoder: Decoder): FirestoreFeedItem {
            val type = decoder.decodeStructure(descriptor) {
                val index = decodeElementIndex(descriptor)
                check(index == 0)
                decodeSerializableElement(descriptor, 0, ItemType.serializer())
            }

            return when (type) {
                ItemType.SPHERE_CHAT -> decoder.decodeSerializableValue(Chat.serializer())
                ItemType.SPHERE_EVENT -> decoder.decodeSerializableValue(Event.serializer())
            }
        }

        override fun serialize(encoder: Encoder, value: FirestoreFeedItem)
            = error("No serialization support for FeedItem.")
    }


    @Serializable
    @SerialName("SPHERE_CHAT")
    data class Chat(
        @SerialName(DOCUMENT_ID_NAME)
        override val id: String,
        override val createdAt: Instant,
        override val updatedAt: Instant?,
        override val prioritized: Boolean,
        override val lastActivityAt: Instant,
        override val expiresAt: Instant?,
        override val archived: Boolean,
        val capabilities: Capabilities,
        val chatInfo: ChatInfo
    ): FirestoreFeedItem() {
        @Serializable
        data class ChatInfo(
            val conversationId: String,
            val title: String?,
            val creatorInfo: AttendantInfo,
            val firstDisplayedMessageInfo: MessageInfo?,
            val lastDisplayedMessageInfo: MessageInfo?,
            val sampleAttendantsInfo: Map<String, SampleAttendant>,
            val private: Boolean,
        )

        @Serializable
        data class Capabilities(val archive: BooleanCapability)
    }

    @Serializable
    @SerialName("SPHERE_EVENT")
    data class Event(
        @SerialName(DOCUMENT_ID_NAME)
        override val id: String,
        override val createdAt: Instant,
        override val updatedAt: Instant?,
        override val prioritized: Boolean,
        override val lastActivityAt: Instant,
        override val expiresAt: Instant?,
        override val archived: Boolean,
        val capabilities: Capabilities,
        val status: Status,
        val eventInfo: EventInfo,
        val chatInfo: ChatInfo?
    ): FirestoreFeedItem() {
        @Serializable
        data class EventInfo(
            val eventId: String,
            val name: String,
            val startDate: Instant,
            val endDate: Instant?,
            val rsvp: Rsvp?,
        )

        @Serializable
        data class ChatInfo(
            val conversationId: String,
            val creatorInfo: AttendantInfo,
            val firstDisplayedMessageInfo: MessageInfo?,
            val lastDisplayedMessageInfo: MessageInfo?,
            val sampleAttendantsInfo: Map<String, SampleAttendant>,
        )
        @Serializable
        data class Capabilities(val rsvp: BooleanCapability, val archive: BooleanCapability)

        @Serializable
        enum class Status {
            DEFAULT, PENDING_RSVP
        }

        @Serializable
        enum class Rsvp {
            GOING, THINKING, NOT_GOING
        }
    }

    @Serializable
    data class MessageInfo(
        val sentAt: Instant,
        val content: Content,
        val senderInfo: AttendantInfo
    ) {
        @Serializable
        data class Content(val text: String)
    }

    @Serializable
    data class SampleAttendant(
        val followedAt: Instant,
        val displayName: String,
        val displayColor: String,
        val displayImageId: String?
    )

    @Serializable
    data class AttendantInfo(
        val attendantId: String,
        val displayName: String,
        val displayColor: String,
        val displayImageId: String?
    )

    @Serializable
    data class BooleanCapability(
        val permitted: Boolean
    )
}
