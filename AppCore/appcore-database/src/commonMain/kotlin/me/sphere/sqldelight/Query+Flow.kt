package me.sphere.sqldelight

import com.squareup.sqldelight.Query
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import me.sphere.appcore.utils.*
import kotlin.time.*

fun <RowType: Any> Query<RowType>.listenAll(option: ListenerOption = ListenerOption.DefaultThrottle): Flow<List<RowType>>
    = triggerFlow().let(option.modifier()).map { executeAsList() }.distinctUntilChanged()

fun <RowType: Any> Query<RowType>.listenOne(option: ListenerOption = ListenerOption.DefaultThrottle): Flow<RowType>
    = triggerFlow().let(option.modifier()).map { executeAsOne() }.distinctUntilChanged()

fun <RowType: Any> Query<RowType>.listenOneOrNull(option: ListenerOption = ListenerOption.DefaultThrottle): Flow<RowType?>
    = triggerFlow().let(option.modifier()).map { executeAsOneOrNull() }.distinctUntilChanged()

fun <T: Any> Query<T>.isNotEmpty(): Boolean = execute { it.next() }

/**
 * Listen to the query for exactly one result, and stop when the result set becomes empty. If the query does not resolve
 * to exactly one result when started, the flow ends immediately.
 *
 * This is almost equivalent to [listenOneOrNull] with [filterNotNull] applied, except that the listener stops as soon
 * as the first `null` is encountered.
 *
 * Use [listenOneOrNull] if you need a sentinel `null` for detecting absence of data.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <RowType: Any> Query<RowType>.listenOneUntilNull(option: ListenerOption = ListenerOption.DefaultThrottle): Flow<RowType>
        = triggerFlow().let(option.modifier()).map { executeAsOneOrNull() }
            .transformWhile {
                if (it != null) {
                    emit(it)
                    true
                } else {
                    false
                }
            }
            .distinctUntilChanged()

@OptIn(ExperimentalTime::class, FlowPreview::class)
sealed class ListenerOption {
    /**
     * Apply the default throttling, which is 250ms at the moment but is subject to change.
     */
    object DefaultThrottle: ListenerOption()
    object NoThrottle: ListenerOption()

    data class CustomThrottle(val window: Duration): ListenerOption()

    internal fun modifier(): (Flow<Unit>) -> Flow<Unit> = when (this) {
        DefaultThrottle -> { { it.throttle(250) } }
        NoThrottle -> { { it } }
        is CustomThrottle -> { { it.throttle(this.window.toLongMilliseconds()) } }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun <RowType: Any> Query<RowType>.triggerFlow(): Flow<Unit>
    = channelFlow<Unit> {
        val listener = object : Query.Listener {
            override fun queryResultsChanged() {
                try {
                    offer(Unit)
                } catch (e: Throwable) {
                    close(e)
                }
            }
        }.freeze()

        addListener(listener)

        try {
            offer(Unit)
        } catch (e: Throwable) {
            close(e)
        }

        awaitClose { removeListener(listener) }
    }
    .buffer(CONFLATED)
