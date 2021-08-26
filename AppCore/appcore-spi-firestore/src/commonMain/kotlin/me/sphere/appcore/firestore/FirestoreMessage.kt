@file:UseSerializers(InstantFirestoreSerializer::class)

package me.sphere.appcore.firestore

import kotlinx.datetime.Instant
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

@Serializable
data class FirestoreMessage(
    @SerialName(DOCUMENT_ID_NAME)
    val id: String,
    val uniqueId: String,
    val conversationId: String,
    val createdAt: Instant,
    val updatedAt: Instant? = null,
    val type: Type,
    val serviceMessageType: ServiceMessageType? = null,
    val content: Content? = null,
    val senderInfo: SenderInfo? = null,
    val important: Boolean? = null,
    val removed: Boolean,
    val mentions: List<Mention>? = null,
    val reactions: List<Reaction>? = null,
    val linkPreviews: List<LinkPreview>? = null,
    val replyToMessageInfo: ReplyToMessageInfo? = null,
    val actions: ActionsPayload? = null,
    val actionableUntil: Instant? = null,
    val permitsMultiplePollVotes: Boolean? = null,
    val permitsActionFromSender: Boolean? = null,
    val syncedShareInfo: SyncedShareInfo? = null
) {
    @Serializable
    enum class Type {
        @SerialName("client_message")
        Client,

        @SerialName("service_message")
        Service
    }

    @Serializable
    enum class ServiceMessageType {
        @SerialName("user_joined")
        UserJoined,

        @SerialName("user_left")
        UserLeft
    }

    @Serializable
    data class Content(
        val type: Type,
        val text: String? = null,
        val imageFilename: String? = null,
        val imageDimensions: ImageDimensions? = null,
        val pollOptions: List<PollOption>? = null,
        val attachments: List<Attachment>? = null
    ) {
        @Serializable
        enum class Type {
            @SerialName("text")
            Text,
            @SerialName("image")
            Image,
            @SerialName("poll")
            Poll
        }

        @Serializable
        data class Attachment(
            // Treat the attachment as raw if the field is absent.
            val attachmentType: AttachmentType = AttachmentType.Raw,
            val resourceType: String,
            val fileId: String,
            val fileName: String,
            val fileSize: Int,
            val dimensions: ImageDimensions? = null
        )

        @Serializable(with = AttachmentType.Serializer::class)
        enum class AttachmentType {
            Video,
            Image,
            Raw,
            AnimatedImage;

            object Serializer: KSerializer<AttachmentType> {
                override val descriptor = PrimitiveSerialDescriptor("AttachmentType", PrimitiveKind.STRING)

                override fun deserialize(decoder: Decoder): AttachmentType = when (decoder.decodeString()) {
                    "video" -> Video
                    "image" -> Image
                    "animated_image" -> AnimatedImage
                    else -> Raw
                }

                override fun serialize(encoder: Encoder, value: AttachmentType) = throw UnsupportedOperationException("Encoding is unimplemented")
            }
        }

        @Serializable
        data class ImageDimensions(
            val width: Int,
            val height: Int
        )

        @Serializable
        data class PollOption(
            val optionId: String,
            val optionTitle: String
        )
    }

    @Serializable
    data class SenderInfo(
        val attendantId: String,
        val displayName: String,
        val displayColor: String,
        val displayImageFilename: String? = null,
    )

    @Serializable
    data class Mention(
        val attendantId: String,
        @SerialName("displayName")
        val name: String,
        val offset: Int,
        val length: Int
    )

    @Serializable
    data class LinkPreview(
        val originalURL: String,
        val url: String,
        val title: String? = null,
        val publisher: String? = null,
        val description: String? = null,
        val logo: String? = null,
        val images: List<Image>? = null
    ) {
        @Serializable
        data class Image(
            val url: String
        )
    }

    @Serializable(with = Reaction.Serializer::class)
    sealed class Reaction {
        sealed class Known: Reaction() {
            abstract val attendantId: String
            abstract val displayName: String
            abstract val displayImageId: String?
            abstract val displayColor: String
        }

        @Serializable
        data class Emoji(
            val emojiId: String,
            val emojiImageId: String,
            val color: String,

            override val attendantId: String,
            override val displayName: String,
            override val displayImageId: String?,
            override val displayColor: String,
        ): Known()

        @Serializable
        data class Appreciation(
            val appreciationId: String,
            val appreciationName: String,
            val emojiImageId: String,
            val color: String,

            override val attendantId: String,
            override val displayName: String,
            override val displayImageId: String?,
            override val displayColor: String,
        ): Known()

        @Serializable
        data class Legacy(
            val type: Type,

            override val attendantId: String,
            override val displayName: String,
            override val displayImageId: String?,
            override val displayColor: String
        ): Known() {
            @Serializable(with = Serializer::class)
            enum class Type {
                ANGER, JOY, ADORATION, EXCITEMENT, LIKE, SADNESS;
            }

            object Serializer: KSerializer<Type> {
                override val descriptor = PrimitiveSerialDescriptor("Reaction.Legacy.Type", PrimitiveKind.STRING)
                // Legacy reaction types are stored by backend in lowercase.
                override fun deserialize(decoder: Decoder) = Type.valueOf(decoder.decodeString().uppercase())
                override fun serialize(encoder: Encoder, value: Type) = error("No serialization support for Reaction.Legacy.Type.")
            }
        }

        object Unknown: Reaction()

        @Serializable
        enum class Type {
            EMOJI_REACTION, APPRECIATION_REACTION;
        }

        object Serializer: KSerializer<Reaction> {
            override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Reaction") {
                element("type", Type.serializer().descriptor)
            }

            override fun deserialize(decoder: Decoder): Reaction {
                val typeString = decoder.decodeStructure(descriptor) {
                    val index = decodeElementIndex(descriptor)
                    check(index == 0)
                    decodeStringElement(descriptor, 0)
                }

                // TODO: Remove `uppercase()` if the new types are definitely uppercase.
                return when (runCatching { Type.valueOf(typeString.uppercase()) }.getOrNull()) {
                    Type.EMOJI_REACTION ->
                        decoder.decodeSerializableValue(Emoji.serializer())
                    Type.APPRECIATION_REACTION ->
                        decoder.decodeSerializableValue(Appreciation.serializer())
                    else ->
                        runCatching { decoder.decodeSerializableValue(Legacy.serializer()) }.getOrNull()
                            ?: Unknown
                }
            }

            override fun serialize(encoder: Encoder, value: Reaction) = error("No serialization support for Reaction.")
        }
    }

    @Serializable
    data class ReplyToMessageInfo(
        val messageId: String,
        val content: Content,
        val senderInfo: FirestoreMessage.SenderInfo,
        val mentions: List<Mention>? = null,
        val removed: Boolean,
        val important: Boolean? = null
    )

    @Serializable
    data class ActionsPayload(
        val dismissals: List<String>,
        val acknowledgements: List<String>? = null,
        val pollVotes: Map<String, List<String>>? = null
    )

    @Serializable
    data class SyncedShareInfo(
        val pinShareTargets: List<PinShareTarget> = listOf(),
        val syncedShareSource: SyncedShareSource? = null
    )

    @Serializable
    data class SyncedShareSource(
        val syncedShareType: SyncedShareType,
        val conversationId: String,
        val conversationName: String,
        val messageId: String,
        val messageCreatedAt: Instant
    )

    @Serializable
    enum class SyncedShareType {
        @SerialName("PIN_SHARE")
        PinShare;
    }

    @Serializable
    data class PinShareTarget(
        val conversationId: String,
        val messageId: String
    )
}
