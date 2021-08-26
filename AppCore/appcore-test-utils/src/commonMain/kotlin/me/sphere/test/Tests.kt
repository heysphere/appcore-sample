package me.sphere.test

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import me.sphere.test.support.*
import kotlin.reflect.KClass
import kotlin.test.*

@OptIn(ExperimentalUnsignedTypes::class)
abstract class Tests {
    val defaultScope get() = CoroutineScope(job + Dispatchers.Default)
    private val job = SupervisorJob()

    @AfterTest
    open fun cleanup() = job.cancel()

    /**
     * A flow that never emits a value during the test run.
     *
     * Note that this will terminate after the test so that any resource is released as expected.
     */
    fun <T> neverFlow(): Flow<T> = flow {
        val job = CompletableDeferred<Unit>(this@Tests.job)

        try {
            job.await()
        } catch (e: Throwable) {
            assertTrue(e is CancellationException, "`neverFlow()` should only fail due to parent job cancellation.")
        }
    }

    /**
     * A suspendable block that never emits a value during the test run.
     *
     * Note that this will terminate after the test so that any resource is released as expected.
     */
    suspend fun <T> suspendForever(): T {
        val job = CompletableDeferred<T>(this@Tests.job)
        return job.await()
    }

    fun busyWait(timeoutMillis: Int = 1000, sleepMillis: Int = 100, assert: BusyWaitAsserter.() -> Unit) {
        var failures = emptyList<String>()

        platformBusyWaitImpl(
            timeoutMillis = timeoutMillis,
            sleepMillis = sleepMillis,
            doAction = {
                val asserter = BusyWaitAsserter()

                try {
                    assert(asserter)
                    failures = asserter.failures
                } catch (e: Throwable) {
                    failures = listOf("${e::class.simpleName!!}: " + e.message + "\n" + e.stackTraceToString())
                }
            },
            repeatWhile = { failures.isNotEmpty() },
            onTimeout = {
                fail("""Busy waiting assertion has timed out after $timeoutMillis milliseconds, because some of the assertions do not pass.
|                   [Failures]
|                   ${failures.joinToString(separator = "\n", prefix = "- ")}
|                   """.trimMargin())
            }
        )
    }

    inner class BusyWaitAsserter {
        var failures = emptyList<String>().toMutableList()

        fun <T> assertEquals(lhs: T, rhs: T, description: String? = null) {
            if (lhs != rhs) {
                val desc = description ?: "value to equal to `$rhs`"
                failures.add("Expect $desc. Got `$lhs` instead.")
            }
        }

        fun <T: Any> assertType(lhs: T?, rhs: KClass<*>, description: String? = null) {
            if (lhs == null) {
                val desc = description ?: "value to be an instance of `${rhs.simpleName}`"
                failures.add("Expect $desc. Got null instead.")
                return
            }

            if (lhs::class != rhs) {
                val desc = description ?: "value to be an instance of `${rhs.simpleName}`"
                failures.add("Expect $desc. Got `${lhs::class.simpleName}` instead.")
            }
        }

        fun FlowTestHelper<*>.assertFlowCompleted(description: String? = null) {
            if (status !is FlowStatus.Completed) {
                val desc = description ?: "the flow to complete successfully."
                failures.add("Expect $desc. Got a flow status of $status instead.")
            }
        }

        inline fun <reified T: Throwable> FlowTestHelper<*>.assertFlowFailure(description: String? = null) {
            val status = this.status
            val failed = status as? FlowStatus.Failed

            if (!(failed != null && failed.throwable is T)) {
                val desc = description ?: "the flow to fail with exception `${T::class.simpleName}`."
                failures.add("Expect $desc. Got a flow status of $status instead.")
            }
        }

        fun assertTrue(value: Boolean?, description: String? = null) {
            if (value != true) {
                val desc = description ?: "boolean value to be true"
                failures.add("Expect $desc. Got $value instead.")
            }
        }

        fun assertFalse(value: Boolean?, description: String? = null) {
            if (value != false) {
                val desc = description ?: "boolean value to be false"
                failures.add("Expect $desc. Got $value instead.")
            }
        }
    }
}
