package me.sphere.appcore.dataSource

import kotlinx.coroutines.flow.map
import me.sphere.appcore.Projection
import me.sphere.appcore.asProjection

/**
 * A computed, self-updating view of an object, or a query of a single result.
 *
 * Use [singleDataSource] to build an instance.
 */
abstract class DataSource<Type: Any> {
    abstract val state: Projection<State<Type>>
    abstract fun refreshNow()

    sealed class State<out Type: Any> {
        object Loading : State<Nothing>()
        data class Value<Type: Any>(val value: Type) : State<Type>()
        data class Failed(val error: Throwable) : State<Nothing>()
    }
}

/**
 * A computed, self-updating view of a list query.
 *
 * Use [listDataSource] to build an instance.
 */
abstract class ListDataSource<Type: Any>: DataSource<List<Type>>()

/**
 * Lift a [DataSource] to be a [ListDataSource], usually as the last step in the composition.
 */
internal fun <T: Any, C: Collection<T>> DataSource<C>.toListDataSource(): ListDataSource<T> = object: ListDataSource<T>() {
    override val state: Projection<State<List<T>>>
        get() = this@toListDataSource.state
            .map { state ->
                @Suppress("UNCHECKED_CAST")
                when (state) {
                    is State.Value -> State.Value(
                        if (state.value is List<*>)
                            state.value as List<T>
                        else
                            state.value.toList()
                    )
                    is State.Failed -> state
                    is State.Loading -> state
                }
            }
            .asProjection()

    override fun refreshNow() = this@toListDataSource.refreshNow()
}
