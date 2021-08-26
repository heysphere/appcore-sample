package me.sphere.appcore.chat

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.datetime.Instant
import me.sphere.appcore.stubs.ConversationEventStub as Stub
import me.sphere.models.ConversationId
import me.sphere.models.chat.ConversationEventFilter
import me.sphere.models.chat.RemoteUpdateMark
import me.sphere.sqldelight.chat.ConversationEvent
import me.sphere.test.LoopTests
import me.sphere.test.support.TestLogger
import kotlin.test.*
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
internal class UpdateMachineTests : LoopTests<UpdateMachine, UpdateMachine.State, UpdateMachine.Event, ConversationEnvironment>(UpdateMachine) {
    private val gateway = StubConversationGateway()

    override val environment = ConversationEnvironment(
        conversationId = ConversationId("stub"),
        filter = ConversationEventFilter.None,
        gateway = gateway,
        logger = TestLogger
    )

    //region Monitor Activation
    @Test
    fun monitoring_is_paused_when_there_is_no_message() = initial().runTesting { initial ->
        assertEquals(initial, state.first())

        // Monitoring should be active by default.
        assertEquals(UpdateMachine.MonitorStatus.Active, initial.monitor)
    }

    @Test
    fun when_the_first_message_appears__activate_monitoring__but_without_starting_the_fetcher_afterwards() {
        val messageTime = Instant.fromEpochSeconds(0, 0)
        val messageTimeAsMark = RemoteUpdateMark(messageTime)
        val initialHead = RemoteUpdateMark(Instant.fromEpochSeconds(-1024, 0))
        require(initialHead.updatedAt < messageTime)

        monitoring_starts_with_many_message__and_maybe_start_the_fetcher(
            events = listOf(
                Stub.textMessage(createdAt = messageTime, lastInSync = messageTimeAsMark)
            ),
            initialHead = initialHead,
            expectedFetcherStart = null,
            expectedFinalHead = messageTimeAsMark
        )
    }

    @Test
    fun when_the_first_message_appears__activate_monitoring_and_then_start_the_fetcher_due_to_observed_discrepencies_in_the_marks() {
        val messageTime = Instant.fromEpochSeconds(0, 0)
        val messageTimeAsMark = RemoteUpdateMark(messageTime)
        val initialHead = RemoteUpdateMark(Instant.fromEpochSeconds(1024, 0))
        require(messageTime < initialHead.updatedAt)

        monitoring_starts_with_many_message__and_maybe_start_the_fetcher(
            events = listOf(
                Stub.textMessage(createdAt = messageTime, lastInSync = messageTimeAsMark)
            ),
            initialHead = initialHead,
            expectedFetcherStart = messageTimeAsMark,
            expectedFinalHead = initialHead
        )
    }

    @Test
    fun when_the_first_batch_of_messages_appears__activate_monitoring__but_without_starting_the_fetcher_afterwards() {
        val events = Stub.textMessagesUsingTimeBank()
        monitoring_starts_with_many_message__and_maybe_start_the_fetcher(
            events = events,
            initialHead = RemoteUpdateMark(Stub.timeBeforeTimeBank),
            expectedFetcherStart = null,
            expectedFinalHead = events.minOf { it.lastInSync },
        )
    }

    @Test
    fun when_the_first_batch_of_message_appears__activate_monitoring_and_then_start_the_fetcher_due_to_observed_discrepencies_in_the_marks() {
        monitoring_starts_with_many_message__and_maybe_start_the_fetcher(
            events = Stub.textMessagesUsingTimeBank(),
            initialHead = RemoteUpdateMark(Stub.shortTimeAfterTimeBank),
            expectedFetcherStart = RemoteUpdateMark(Stub.timeBank[0]),
            expectedFinalHead = RemoteUpdateMark(Stub.shortTimeAfterTimeBank)
        )
    }

    private fun monitoring_starts_with_many_message__and_maybe_start_the_fetcher(
        events: List<ConversationEvent>,
        initialHead: RemoteUpdateMark,
        expectedFetcherStart: RemoteUpdateMark?,
        expectedFinalHead: RemoteUpdateMark
    ) = initial()
        .copy(container = ConversationEventsContainer(events))
        .runTesting { initial ->
            val fakeMarkSource = MutableSharedFlow<RemoteUpdateMark>(1)

            gateway.stubMonitorUpdates.set { conversationId ->
                assertEquals(environment.conversationId, conversationId)
                fakeMarkSource
            }
            gateway.stubFetchUpdatedMessages.set { start, _, range ->
                assertEquals(expectedFetcherStart!!, actual = start)
                assertEquals(initial.container.range!!, actual = range)

                suspendForever()
            }

            state.test {
                assertEquals(initial, actual = expectItem())
                assertEquals(
                    initial.copy(
                        monitor = UpdateMachine.MonitorStatus.Active,
                        marks = UpdateMachine.Marks(remoteHead = events.minOf { it.lastInSync })
                    ),
                    actual = expectItem()
                ) // DidDetectLastInSyncChange

                fakeMarkSource.emit(initialHead)

                assertEquals(
                    initial.copy(
                        monitor = UpdateMachine.MonitorStatus.Active,
                        marks = UpdateMachine.Marks(remoteHead = expectedFinalHead),
                        fetcher = when (expectedFetcherStart) {
                            null -> UpdateMachine.FetcherStatus.Idle
                            else -> UpdateMachine.FetcherStatus.Fetching(
                                startExclusive = expectedFetcherStart,
                                range = initial.container.range!!
                            )
                        }
                    ),
                    actual = expectItem()
                ) // MonitorDidNotify
            }
        }
    //endregion

    //region Fetcher Response
    @Test
    fun fetcher_stops_after_a_successful_response_bringing_all_lastInSync_marks_up_to_remote_head() = initial()
        .copy(
            container = ConversationEventsContainer(Stub.textMessagesUsingTimeBank()),
            marks = UpdateMachine.Marks(remoteHead =  RemoteUpdateMark(Stub.shortTimeAfterTimeBank)),
            monitor = UpdateMachine.MonitorStatus.Active,
            fetcher = UpdateMachine.FetcherStatus.Fetching(
                startExclusive = RemoteUpdateMark(Stub.timeBank[0]),
                range = Stub.timeBankRange
            )
        )
        .also {
            gateway.stubMonitorUpdates.set { neverFlow() }
            gateway.stubFetchUpdatedMessages.set { startExclusive, id, range ->
                assertEquals(RemoteUpdateMark(Stub.timeBank[0]), startExclusive)
                assertEquals(id, environment.conversationId)
                assertEquals(range, Stub.timeBankRange)

                val updatedEvents = Stub.textMessagesUsingTimeBank(
                    updatedAt = RemoteUpdateMark(Stub.shortTimeAfterTimeBank),
                    lastInSync = RemoteUpdateMark(Stub.longTimeAfterTimeBank)
                )
                ConversationGateway.UpdateFetchResult(updatedEvents, RemoteUpdateMark(Stub.longTimeAfterTimeBank))
            }
        }
        .runTesting { initial ->
            state.test {
                assertEquals(initial, actual = expectItem())

                val expectedState = initial.copy(
                    container = ConversationEventsContainer(
                        Stub.textMessagesUsingTimeBank(
                            updatedAt = RemoteUpdateMark(Stub.shortTimeAfterTimeBank),
                            lastInSync = RemoteUpdateMark(Stub.longTimeAfterTimeBank)
                        )
                    ),
                    marks = UpdateMachine.Marks(remoteHead = RemoteUpdateMark(Stub.longTimeAfterTimeBank)),
                    fetcher = UpdateMachine.FetcherStatus.Idle
                )

                assertEquals(expectedState.container.minLastInSync, RemoteUpdateMark(Stub.longTimeAfterTimeBank))
                assertEquals(expectedState, actual = expectItem()) // FetcherDidReceive
                assertEquals(expectedState, actual = expectItem()) // DidDetectLastInSyncChange
            }
        }

    private val responseValve = Semaphore(1, 1)

    @Test
    fun fetcher_stops_if_a_successful_response_does_not_cause_the_container_minLastInSync_to_progress() = initial()
        .copy(
            /**
             * [ Boundary case ]
             * container.minLastSync                                    // What populates the Fetching state.
             *   equals (fetcher as Fetching).startExclusive            // The Fetching state itself.
             *   eventuallyEquals (event as FetcherDidReceive).newMark  // The async response giving back the same
             *                                                          // RemoteUpdateMark, i.e., making no progress.
             */
            container = ConversationEventsContainer(
                Stub.textMessagesUsingTimeBank(lastInSync = Stub.shortTimeAfterTimeBank.let(::RemoteUpdateMark))
            ),
            marks = UpdateMachine.Marks(remoteHead = RemoteUpdateMark(Stub.longTimeAfterTimeBank)),
            monitor = UpdateMachine.MonitorStatus.Active,
            fetcher = UpdateMachine.FetcherStatus.Fetching(
                startExclusive = Stub.shortTimeAfterTimeBank.let(::RemoteUpdateMark),
                range = Stub.timeBankRange
            )
        )
        .also {
            gateway.stubMonitorUpdates.set { neverFlow() }
            gateway.stubFetchUpdatedMessages.set { _, _, _ ->
                // Suspend until the testing coroutine to give us a green light.
                responseValve.acquire()

                /**
                 * We return empty list and [Stub.shortTimeAfterTimeBank] here. Since this "new" mark is equivalent
                 * to what we set in the initial state, if we compute a new [UpdateMachine.FetcherStatus.Fetching] state
                 * now, it will be identical to the current [UpdateMachine.State.fetcher] value, thus causing the fetcher
                 * process to be stuck — if untreated — due to `skippingRepeated()` never ever being triggered again.
                 *
                 * [UpdateMachine] should actively check for this scenario, and force the next state to be [UpdateMachine.FetcherStatus.Idle].
                 * This way the [UpdateMachine] can issue a new fetch, when the next opportunity arises (either remote
                 * head is changed, or the container minLastInSync is changed).
                 */
                ConversationGateway.UpdateFetchResult(
                    emptyList(),
                    Stub.shortTimeAfterTimeBank.let(::RemoteUpdateMark)
                )
            }
        }
        .runTesting { initial ->
            state.test {
                assertEquals(initial, actual = expectItem())

                assertEquals(initial, actual = expectItem()) // DidDetectLastInSyncChange
                // Since we did start with non-null lastInSync, one of the skippingRepeated effects is triggered and
                // enqueued this event.

                // Give `stubFetchUpdatedMessages` a green light to proceed to return the stubbed response.
                responseValve.release()

                val expectedState = initial.copy(fetcher = UpdateMachine.FetcherStatus.Idle)
                assertEquals(expectedState, actual = expectItem()) // FetcherDidReceive
            }
        }

    @Test
    fun fetcher_continues_with_a_new_iteration_after_a_successful_response__since_the_minimum_lastInSync_still_falls_behind() = initial()
        .copy(
            container = ConversationEventsContainer(Stub.textMessagesUsingTimeBank()),
            marks = UpdateMachine.Marks(remoteHead =  RemoteUpdateMark(Stub.reallyLongTimeAfterTimeBank)),
            monitor = UpdateMachine.MonitorStatus.Active,
            fetcher = UpdateMachine.FetcherStatus.Fetching(
                startExclusive = RemoteUpdateMark(Stub.timeBank[0]),
                range = Stub.timeBankRange
            )
        )
        .also {
            gateway.stubMonitorUpdates.set { neverFlow() }
            gateway.stubFetchUpdatedMessages.set { startExclusive, id, range ->
                assertEquals(RemoteUpdateMark(Stub.timeBank[0]), startExclusive)
                assertEquals(id, environment.conversationId)
                assertEquals(range, Stub.timeBankRange)

                val updatedEvents = Stub.textMessagesUsingTimeBank(
                    updatedAt = RemoteUpdateMark(Stub.shortTimeAfterTimeBank),
                    lastInSync = RemoteUpdateMark(Stub.shortTimeAfterTimeBank)
                )
                ConversationGateway.UpdateFetchResult(updatedEvents, RemoteUpdateMark(Stub.shortTimeAfterTimeBank))
            }
        }
        .runTesting { initial ->
            state.test {
                assertEquals(initial, actual = expectItem())

                val expectedState = initial.copy(
                    container = ConversationEventsContainer(
                        Stub.textMessagesUsingTimeBank(
                            updatedAt = RemoteUpdateMark(Stub.shortTimeAfterTimeBank),
                            lastInSync = RemoteUpdateMark(Stub.shortTimeAfterTimeBank)
                        )
                    ),
                    marks = UpdateMachine.Marks(remoteHead = RemoteUpdateMark(Stub.reallyLongTimeAfterTimeBank)),
                    fetcher = UpdateMachine.FetcherStatus.Fetching(
                        startExclusive = RemoteUpdateMark(Stub.shortTimeAfterTimeBank),
                        range = Stub.timeBankRange
                    )
                )

                assertEquals(expectedState.container.minLastInSync, RemoteUpdateMark(Stub.shortTimeAfterTimeBank))
                assertEquals(expectedState, actual = expectItem()) // FetcherDidReceive
                assertEquals(expectedState, actual = expectItem()) // DidDetectLastInSyncChange
            }
        }

    @Test
    fun fetcher_stops_after_a_failure_response() = initial()
        .copy(
            container = ConversationEventsContainer(Stub.textMessagesUsingTimeBank()),
            marks = UpdateMachine.Marks(remoteHead = RemoteUpdateMark(Stub.shortTimeAfterTimeBank)),
            monitor = UpdateMachine.MonitorStatus.Active,
            fetcher = UpdateMachine.FetcherStatus.Fetching(
                startExclusive = RemoteUpdateMark(Stub.timeBank[0]),
                range = Stub.timeBankRange
            )
        )
        .also {
            gateway.stubMonitorUpdates.set { neverFlow() }
            gateway.stubFetchUpdatedMessages.set { _, _, _ -> throw IllegalStateException("stub") }
        }
        .runTesting { initial ->
            state.test {
                assertEquals(initial, actual = expectItem())

                val expectedState = initial.copy(fetcher = UpdateMachine.FetcherStatus.Failed)
                assertEquals(expectedState.container.minLastInSync, RemoteUpdateMark(Stub.timeBank[0]))
                assertEquals(expectedState, actual = expectItem()) // FetcherDidFail
                assertEquals(expectedState, actual = expectItem()) // DidDetectLastInSyncChange
            }
        }
    //endregion

    private fun initial() = UpdateMachine.State(
        container = ConversationEventsContainer(),
        marks = UpdateMachine.Marks(),
        monitor = UpdateMachine.MonitorStatus.Active,
        fetcher = UpdateMachine.FetcherStatus.Idle
    )
}
