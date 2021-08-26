package me.sphere.appcore.chat

import app.cash.turbine.FlowTurbine
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import me.sphere.appcore.utils.Atomic
import me.sphere.models.ConversationId
import me.sphere.models.chat.ConversationEventFilter
import me.sphere.models.chat.LocalUpdateMark
import me.sphere.models.chat.RemoteConversationEventId
import me.sphere.test.LoopTests
import me.sphere.test.support.TestLogger
import kotlin.test.*
import kotlin.time.ExperimentalTime
import me.sphere.appcore.stubs.ConversationEventStub as Stub

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
internal class LocalUpdateMachineTests : LoopTests<LocalUpdateMachine, LocalUpdateMachine.State, LocalUpdateMachine.Event, ConversationEnvironment>(LocalUpdateMachine) {
    private val gateway = StubConversationGateway()
    private val headMark = MutableSharedFlow<LocalUpdateMark>(replay = 1)

    override val environment = ConversationEnvironment(
        conversationId = ConversationId("stub"),
        filter = ConversationEventFilter.None,
        gateway = gateway,
        logger = TestLogger
    )

    @Test
    fun should_fetch_updates_continuously_as_the_head_mark_moves() = initial()
        .also {
            gateway.stubStreamHardDeleted.set { neverFlow() }
            gateway.stubMonitorLocalUpdates.set { headMark }
        }
        .runTesting {
            state.test {
                assertEquals(initial(), actual = expectItem())

                //region INITIAL -> 1024
                assertNormalMarkChange(
                    initialAcknowledged = null,
                    headMarkToEmit = LocalUpdateMark(1024),
                    newMarkInFetchResponse = LocalUpdateMark(1024)
                )
                //endregion

                //region 1024 -> 4096
                assertNormalMarkChange(
                    initialAcknowledged = LocalUpdateMark(1024),
                    headMarkToEmit = LocalUpdateMark(4096),
                    newMarkInFetchResponse = LocalUpdateMark(4096)
                )
                //endregion

                //region 4096 -> 8192
                assertNormalMarkChange(
                    initialAcknowledged = LocalUpdateMark(4096),
                    headMarkToEmit = LocalUpdateMark(8192),
                    newMarkInFetchResponse = LocalUpdateMark(8192)
                )
                //endregion
            }
        }

    @Test
    fun should_progressively_fetch_local_updates_until_newMark_in_fetch_response_agrees_with_head() = initial()
        .also {
            gateway.stubStreamHardDeleted.set { neverFlow() }
            gateway.stubMonitorLocalUpdates.set { headMark }
        }
        .runTesting {
            state.test {
                assertEquals(initial(), actual = expectItem())

                assertMarkChange(
                    headMarkToEmit = LocalUpdateMark(1024),
                    responseSequence = listOf(
                        // First fetch:
                        // Initial is null; fetch response gives newMark=512, which is smaller than head=1024.
                        FetchResponseAssertion(
                            expectedRequestMark = null,
                            response = ConversationGateway.UpdateFetchResult(emptyList(), LocalUpdateMark(512))
                        ),
                        // Second fetch:
                        // Fetches localUpdatedAt > 512; fetch response gives newMark=999, which is smaller than head=1024.
                        FetchResponseAssertion(
                            expectedRequestMark = LocalUpdateMark(512),
                            response = ConversationGateway.UpdateFetchResult(emptyList(), LocalUpdateMark(999))
                        ),
                        // Third fetch:
                        // Fetches localUpdatedAt > 999; fetch response gives newMark=1024, which now agrees with the head.
                        FetchResponseAssertion(
                            expectedRequestMark = LocalUpdateMark(999),
                            response = ConversationGateway.UpdateFetchResult(emptyList(), LocalUpdateMark(1024))
                        )
                    )
                )
            }
        }

    @Test
    fun process_hard_deletes() = initial()
        .copy(container = ConversationEventsContainer(Stub.textMessagesUsingTimeBank()))
        .also {
            gateway.stubStreamHardDeleted.set { hardDeletions }
            gateway.stubMonitorLocalUpdates.set { neverFlow() }
        }
        .runTesting { initial ->
            state.test {
                assertEquals(initial, actual = expectItem())
                assertTrue(initial.container.events.isNotEmpty())

                hardDeletions.emit(Stub.textMessagesUsingTimeBank().map { it.eventId })
                assertEquals(
                    initial.copy(container = ConversationEventsContainer(emptyList())),
                    actual = expectItem()
                )
            }
        }

    private val hardDeletions = MutableSharedFlow<List<RemoteConversationEventId>>(replay = 1)

    private suspend fun FlowTurbine<LocalUpdateMachine.State>.assertNormalMarkChange(
        initialAcknowledged: LocalUpdateMark?,
        headMarkToEmit: LocalUpdateMark,
        newMarkInFetchResponse: LocalUpdateMark
    ) {
        assertMarkChange(
            headMarkToEmit = headMarkToEmit,
            responseSequence = listOf(
                FetchResponseAssertion(
                    expectedRequestMark = initialAcknowledged,
                    response = ConversationGateway.UpdateFetchResult(emptyList(), newMarkInFetchResponse)
                )
            )
        )
    }

    private suspend fun FlowTurbine<LocalUpdateMachine.State>.assertMarkChange(
        headMarkToEmit: LocalUpdateMark,
        responseSequence: List<FetchResponseAssertion>,
    ) {
        val responseIndex = Atomic(0)

        gateway.stubFetchLocallyUpdatedMessages.set { mark, conversationId, range ->
            val assertion = responseSequence[responseIndex.value++]
            val expectedPreviousMark = assertion.expectedRequestMark ?: LocalUpdateMark.INITIAL
            assertEquals(expectedPreviousMark, actual = mark)
            assertEquals(environment.conversationId, actual = conversationId)
            assertEquals(Stub.timeBankRange, actual = range)

            assertion.response
        }

        headMark.emit(headMarkToEmit)
        assertEquals(
            initial().copy(head = headMarkToEmit, acknowledged = responseSequence.first().expectedRequestMark),
            actual = expectItem() // Event.HeadMarkDidChange
        )

        responseSequence.forEachIndexed { index, assertion ->
            assertEquals(
                initial().copy(head = headMarkToEmit, acknowledged = assertion.response.newMark),
                actual = expectItem(), // Event.DidFetchUpdates
                message = "UpdateFetchResult $index in 0..<${responseSequence.count()} does not cause state update in the expected manner."
            )
        }
    }

    data class FetchResponseAssertion(
        val expectedRequestMark: LocalUpdateMark?,
        val response: ConversationGateway.UpdateFetchResult<LocalUpdateMark>
    )

    private fun initial() = LocalUpdateMachine.State(
        container = ConversationEventsContainer(Stub.textMessagesUsingTimeBank()),
        head = LocalUpdateMark.INITIAL,
        acknowledged = null
    )
}
