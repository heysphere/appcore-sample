package me.sphere.appcore.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlin.experimental.ExperimentalTypeInference
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.time.*
import kotlin.time.TimeSource.Monotonic

fun <T> Flow<T>.exponentialBackoffRetry(
    maxAttempt: Long = 128,
    shouldRetry: ((Throwable) -> Boolean)? = null
): Flow<T> = retryWhen { cause, attempt ->
    val canRetry = shouldRetry?.invoke(cause) ?: true

    if (canRetry && attempt < maxAttempt) {
        // Max backoff time = 128 seconds ~= 2 minutes
        val baseSeconds = 1L shl min(attempt.toInt(), 7)
        delay(baseSeconds * 1000 + Random.Default.nextLong(1000))
        true
    } else {
        false
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.timeout(timeMillis: Long, exception: () -> Throwable): Flow<T> = callbackFlow {
    val timeoutJob = launch {
        delay(timeMillis)

        if (isActive) {
            close(exception())
        }
    }

    this@timeout
        .onEach {
            if (timeoutJob.isActive) {
                timeoutJob.cancel()
            }

            send(it)
        }
        .onCompletion { close(it) }
        .collect()

    awaitClose { timeoutJob.cancel() }
}

fun <T> Flow<T>.onError(action: suspend FlowCollector<T>.(Throwable) -> Unit): Flow<T> = onCompletion {
    if (it != null && it !is CancellationException) action(it)
}

fun <T> Flow<T>.ignoreError(): Flow<T> = catch {}

class TimeoutException(override val message: String? = null): RuntimeException(message)

@OptIn(ExperimentalStdlibApi::class, ExperimentalTime::class, ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.throttle(minimumIntervalMillis: Long): Flow<T> = callbackFlow<T> {
    var lastEmitted: TimeMark? = null
    var latestValue: T?
    var job: Job? = null

    this@throttle.onCompletion { close(it) }.collect { upstreamValue ->
        val elapsed = lastEmitted?.elapsedNow()?.toLong(DurationUnit.MILLISECONDS) ?: minimumIntervalMillis

        if (job == null && elapsed >= minimumIntervalMillis) {
            lastEmitted = Monotonic.markNow()
            send(upstreamValue)
        } else {
            latestValue = upstreamValue

            if (job == null) {
                val remaining = max(minimumIntervalMillis - elapsed, 0)
                job = launch {
                    delay(remaining)

                    latestValue?.let { value ->
                        lastEmitted = Monotonic.markNow()
                        latestValue = null
                        send(value)
                    }
                    job = null
                }
            }
        }
    }

    awaitClose()
}

fun <T> Flow<T>.combinePrevious(initial: T): Flow<Pair<T, T>> = flow {
    var previous = initial

    emitAll(map {
        val p = previous
        previous = it
        p to it
    })
}

/**
 * Run [transform] on every newly inserted item observed in the collection emitted by the receiver.
 *
 * This operator has no object permanence. So if an item is inserted, then removed, and finally reinserted, the operator
 * runs [transform] again upon re-insertion.
 *
 * Example:
 * ```kotlin
 * val idsToRemind: StateFlow<List<String>>
 *
 * idsToRemind
 *     .transformInsertions { id ->
 *         val receipt = apiClient.remind(id)
 *         emit(receipt)
 *     }
 *     .onEach { println("Reminder receipt: $it") }
 *     .launchIn(ActivityScope)
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTypeInference::class)
inline fun <Element, C: Collection<Element>, R> Flow<C>.transformInsertions(
    @BuilderInference crossinline transform: suspend FlowCollector<R>.(Element) -> Unit
): Flow<R>
    = this
    .scan(CollectionDiffState<Element, Element>()) { state, snapshot ->
        val currentIds = snapshot.toSet()
        val insertedIds = currentIds - state.previousIds
        val newElements = snapshot.filter { insertedIds.contains(it) }
        return@scan CollectionDiffState(currentIds, newElements)
    }
    .transform {
        for (element in it.newElements) {
            transform(element)
        }
    }

@PublishedApi
internal data class CollectionDiffState<Element, Identifier>(
    val previousIds: Set<Identifier> = emptySet(),
    val newElements: List<Element> = emptyList()
)
