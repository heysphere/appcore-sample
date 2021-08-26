package me.sphere.appcore.dataSource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import me.sphere.appcore.Projection
import me.sphere.appcore.asProjection
import me.sphere.appcore.utils.freeze

/**
 * Create a new `[DataSource]` by combining an existing one with a [Flow].
 *
 * If the given [other] flow throws an exception, the composed flow completes exceptionally. Consider materializing
 * [other] into a flow of [Result], if you need to compute the data source state based on whether [other] has thrown
 * an exception.
 */
internal fun <T1 : Any, T2, R : Any> DataSource<T1>.combine(
    other: Flow<T2>,
    transform: (T1, T2) -> DataSource.State<R>
): DataSource<R> = object : DataSource<R>() {
    init {
        freeze()
    }

    override val state: Projection<State<R>>
        get() = this@combine.state
            .combine(other) { value1, value2 ->
                when (value1) {
                    is State.Loading -> State.Loading
                    is State.Value -> runCatching { transform(value1.value, value2) }
                        .getOrElse { State.Failed(it) }
                    is State.Failed -> State.Failed(value1.error)
                }
            }
            .asProjection()

    override fun refreshNow() = this@combine.refreshNow()
}

internal fun <T1 : Any, T2 : Any, R : Any> DataSource<T1>.combine(
    other: DataSource<T2>,
    transform: (T1, T2) -> DataSource.State<R>
): DataSource<R> = object : DataSource<R>() {
    init {
        freeze()
    }

    override val state: Projection<State<R>>
        get() = this@combine.state
            .combine(other.state) { value1, value2 ->
                when (value1) {
                    is State.Loading -> State.Loading
                    is State.Value -> when (value2) {
                        State.Loading -> State.Loading
                        is State.Value -> runCatching { transform(value1.value, value2.value) }
                            .getOrElse { State.Failed(it) }
                        is State.Failed -> State.Failed(value2.error)
                    }
                    is State.Failed -> State.Failed(value1.error)
                }
            }
            .asProjection()

    override fun refreshNow() {
        this@combine.refreshNow()
        other.refreshNow()
    }
}
