package me.sphere.appcore.chat

import kotlinx.datetime.Instant
import me.sphere.appcore.stubs.Conversation
import me.sphere.appcore.stubs.SphereStub
import me.sphere.appcore.usecases.chat.Conversation
import me.sphere.appcore.usecases.chat.MessageActionsUseCase
import me.sphere.models.chat.*
import me.sphere.sqldelight.operations.OperationUtils
import me.sphere.test.DbTests
import me.sphere.test.support.TestLogger
import kotlin.reflect.cast
import kotlin.test.Test
import kotlin.test.assertEquals

internal class TogglePollOptionTests: DbTests() {
    private fun Scope.makeUseCase() = MessageActionsUseCase(database, operationUtils, storeScope)

    @Test
    fun single_choice_poll__should_select_choice_with_no_prior_choice() = runTesting {
        setup(canSelectMulti = false)

        makeUseCase().run {
            togglePollOption(stubConversationId, stubItemId, options[1].id)
            assertEquals(setOf(options[1].id), actual = currentOptions())
        }
    }

    @Test
    fun single_choice_poll__should_override_prior_choice() = runTesting {
        setup(canSelectMulti = false, initialOptions = setOf(options[1].id))

        makeUseCase().run {
            togglePollOption(stubConversationId, stubItemId, options[2].id)
            assertEquals(setOf(options[2].id), actual = currentOptions())
        }
    }

    @Test
    fun single_choice_poll__should_undo_choice() = runTesting {
        setup(canSelectMulti = false, initialOptions = setOf(options[0].id))

        makeUseCase().run {
            togglePollOption(stubConversationId, stubItemId, options[0].id)
            assertEquals(setOf(), actual = currentOptions())
        }
    }

    @Test
    fun multi_choice_poll__should_select_choice_with_no_prior_choice() = runTesting {
        setup(canSelectMulti = true)

        makeUseCase().run {
            togglePollOption(stubConversationId, stubItemId, options[1].id)
            assertEquals(setOf(options[1].id), actual = currentOptions())
        }
    }

    @Test
    fun multi_choice_poll__should_include_new_choice_with_prior_choice() = runTesting {
        setup(canSelectMulti = true, initialOptions = setOf(options[2].id))

        makeUseCase().run {
            togglePollOption(stubConversationId, stubItemId, options[1].id)
            assertEquals(setOf(options[1].id, options[2].id), actual = currentOptions())

            togglePollOption(stubConversationId, stubItemId, options[0].id)
            assertEquals(options.map(PollOption::id).toSet(), actual = currentOptions())
        }
    }

    @Test
    fun multi_choice_poll__should_undo_choice() = runTesting {
        setup(canSelectMulti = true, initialOptions = setOf(options[0].id))

        makeUseCase().run {
            togglePollOption(stubConversationId, stubItemId, options[0].id)
            assertEquals(setOf(), actual = currentOptions())
        }
    }

    private val stubConversationId = "stub-convo"
    private val stubItemId = Conversation.ItemId.Committed.Message("stub-message", "irrelevant")
    private val options = listOf(
        PollOption("opt0", "Option 0"),
        PollOption("opt1", "Option 1"),
        PollOption("opt2", "Option 2"),
    )

    private fun setup(
        canSelectMulti: Boolean,
        initialOptions: Set<String>? = null,
        status: MessageActionLocalStatus = MessageActionLocalStatus.ActionableOrExpired
    ) = database.transaction {
        database.sphereQueries.upsert(SphereStub.spheres[0])
        database.conversationQueries.upsert(
            Conversation(stubConversationId, SphereStub.spheres[0].id, Instant.fromEpochSeconds(128, 256))
        )
        val createdAt = Instant.fromEpochSeconds(128, 256)
        database.conversationEventQueries.upsertByRemoteEventId(
            eventId = RemoteConversationEventId(stubItemId.id),
            conversationId = stubConversationId,
            createdAt = createdAt,
            updatedAt = null,
            lastInSync = RemoteUpdateMark(createdAt),
            important = true,
            removed = false,
            info = PollMessageInfo(
                props = ActionableMessageData(
                    actionableUntil = null,
                    permitsActionFromSender = true,
                    isImportant = true,
                    dismissals = listOf(),
                    localStatus = status,
                    clientGeneratedId = "irrelevant",
                    senderInfo = MessageSenderInfo("", "", null, ""),
                    reactionsInfo = MessageReactionsInfo(),
                    syncedShareInfo = null,
                    linkPreviews = listOf(),
                    localReactorState = LocalMessageReactorState(),
                    isDeletedOptimistically = false
                ),
                text = TextContent("stub", listOf()),
                permitsMultiplePollVotes = canSelectMulti,
                numberOfVoters = 0,
                pollVotes = emptyMap(),
                pollOptions = options,
                uncommittedChoices = initialOptions
            )
        )
    }

    private fun currentOptions(): Set<String> = database
        .conversationEventQueries
        .getByRemoteId(stubConversationId, RemoteConversationEventId(stubItemId.id))
        .executeAsOne()
        .info
        .let(PollMessageInfo::class::cast)
        .uncommittedChoices
        ?: emptySet()
}
