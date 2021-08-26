package me.sphere.appcore.dataSource

import kotlinx.coroutines.flow.map
import me.sphere.appcore.Projection
import me.sphere.appcore.asProjection

/**
 * Create a new `[DataSource]` by transforming its value from the receiver [DataSource].
 */
internal fun <T: Any, R: Any> DataSource<T>.map(
    transform: (T) -> R
): DataSource<R> = object: DataSource<R>() {
    override val state: Projection<State<R>>
        get() = this@map.state
            .map { state ->
                when (state) {
                    is State.Value -> State.Value(transform(state.value))
                    is State.Failed -> state
                    is State.Loading -> state
                }
            }
            .asProjection()

    override fun refreshNow() = this@map.refreshNow()
}
