package me.sphere.appcore

import kotlinx.coroutines.flow.*
import me.sphere.appcore.dataSource.DataSource.State.Value
import me.sphere.appcore.dataSource.PagingDataSource
import me.sphere.appcore.dataSource.PagingState
import me.sphere.appcore.dataSource.PagingStatus
import me.sphere.appcore.dataSource.asDataSource

class StubFactory<Value: Any> {
    fun singleOf(value: Value) = flowOf(value).asSingle()

    fun singleOf(error: Throwable): Single<Value> =
        flow<Value> { throw error }.asSingle()

    fun projectionOf(value: Value) = flowOf(value).asProjection()

    fun dataSourceOf(value: Value, startsWithLoading: Boolean = false) =
        flowOf(value).asDataSource(startsWithLoading = startsWithLoading)

    fun loadingDataSource() = emptyFlow<Value>().asDataSource(startsWithLoading = true)

    fun failedDataSource(error: Throwable, startsWithLoading: Boolean = false) =
        flow<Value> { throw error }.asDataSource(startsWithLoading)

    fun pagingDataSourceOf(state: PagingState<Value>) = object : PagingDataSource<Value>() {
        override val state: Projection<PagingState<Value>>
            get() = flowOf(state).asProjection()

        override fun next() {}
        override fun reload() {}
        override fun close() {}
    }

    fun infinitePagingDataSourceOf(items: List<Value>) = object : PagingDataSource<Value>() {
        private val state_ = MutableStateFlow(items)

        override val state: Projection<PagingState<Value>>
            get() = state_.map { PagingState(items = it, status = PagingStatus.HAS_MORE) }.asProjection()

        override fun next() {
            state_.value = state_.value + items
        }

        override fun reload() {
            state_.value = items
        }

        override fun close() {}
    }
}
