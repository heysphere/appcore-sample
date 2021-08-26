package me.sphere.appcore.usecases

import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import me.sphere.appcore.stubs.*
import me.sphere.appcore.usecases.home.*
import me.sphere.models.chat.ConversationType
import me.sphere.models.chat.LastMessageDigestType
import me.sphere.test.DbTests
import me.sphere.test.support.flowTest
import me.sphere.sqldelight.*
import me.sphere.sqldelight.Sphere
import me.sphere.sqldelight.operations.OperationUtils
import me.sphere.test.support.TestLogger
import kotlin.test.Test

private val END_TIME = Instant.fromEpochSeconds(100000)

class ChatListUseCaseImplTests: DbTests() {
    private val useCase = chatListUseCase(database, OperationUtils(database, TestLogger, StoreScope))

    @Test
    fun listChats() = flowTest<List<ChatList.Chat>> {
        val sphere = SphereStub.spheres[0]

        val stubChat0 = Conversation("convo0", sphere.id, Instant.fromEpochSeconds(0, 0))
        val stubMyAttendant0 = MyAttendant("convo0")

        val stubChat1 = Conversation("convo1", sphere.id, Instant.fromEpochSeconds(128, 0))

        val stubChat2 = Conversation("convo2", sphere.id, Instant.fromEpochSeconds(-1024, 0))
        val stubDigest2 = LastMessageDigest(
            "convo2",
            "convo2-msg0",
            "attendant0",
            "Sender",
            LastMessageDigestType.Text,
            "Digest",
            Instant.fromEpochSeconds(64, 0),
            null,
            isImportant = false
        )

        val feedChat0 = ChatList.Chat(
            id = stubChat0.id,
            cardId = stubChat0.chatId,
            parentSphereId = stubChat0.sphereId,
            title = stubChat0.title,
            imageFilename = stubChat0.imageFilename,
            isMuted = false,
            isFollowing = true,
            unreadCount = 0,
            outstandingImportantMessageCount = 0,
            attendantCount = 1,
            isNew = false,
            isPrivate = false,
            lastMessage = null,
            isDM = false,
            imageColor = null,
            _isCreator = false,
            conversationType = ConversationType.Channel,
            sphereEventId = null
        )

        val feedChat1 = ChatList.Chat(
            id = stubChat1.id,
            cardId = stubChat1.chatId,
            parentSphereId = stubChat0.sphereId,
            title = stubChat1.title,
            imageFilename = stubChat1.imageFilename,
            isMuted = false,
            isFollowing = false,
            unreadCount = 0,
            outstandingImportantMessageCount = 0,
            attendantCount = 1,
            isNew = false,
            isPrivate = false,
            lastMessage = null,
            isDM = false,
            imageColor = null,
            _isCreator = false,
            conversationType = ConversationType.Channel,
            sphereEventId = null
        )

        val feedChat2 = ChatList.Chat(
            id = stubChat2.id,
            cardId = stubChat2.chatId,
            parentSphereId = stubChat0.sphereId,
            title = stubChat2.title,
            imageFilename = stubChat2.imageFilename,
            isMuted = false,
            isFollowing = false,
            unreadCount = 0,
            outstandingImportantMessageCount = 0,
            attendantCount = 1,
            isNew = false,
            isPrivate = false,
            lastMessage = ChatList.LastMessage(
                LastMessageDigestType.Text,
                stubDigest2.digest,
                stubDigest2.senderName,
                stubDigest2.createdAt,
                false,
                isImportant = false
            ),
            isDM = false,
            imageColor = null,
            _isCreator = false,
            conversationType = ConversationType.Channel,
            sphereEventId = null
        )

        database.transaction {
            setLastFetchSuccessScenario(listOf(sphere))

            // Create convo0, which has a MyAttendant.
            stubChat0.apply(database.conversationQueries::upsert)
            stubMyAttendant0.apply(database.myAttendantQueries::insertIfNotExist)

            // Create convo1, which has no existing MyAttendant.
            stubChat1.apply(database.conversationQueries::upsert)

            // Create convo2, which has no existing MyAttendant but a LastMessageDigest.
            stubChat2.apply(database.conversationQueries::upsert)
            stubDigest2.apply(database.lastMessageDigestQueries::upsert)
        }

        launch(useCase.chatList(sphere.id).map { it.chats })

        busyWait {
            assertEquals(values.lastOrNull(), listOf(feedChat1, feedChat2, feedChat0))
        }
    }

    private fun setLastFetchSuccessScenario(spheres: List<Sphere>) {
        require(spheres.isNotEmpty())

        database.transaction {
            SphereStub.spheres.forEach(database.sphereQueries::upsert)
            database.feedPollingStateQueries.actorSetSuccess(END_TIME)
            database.feedPollingStateQueries.setSelectedSphere(spheres[0].id)
        }
    }
}
