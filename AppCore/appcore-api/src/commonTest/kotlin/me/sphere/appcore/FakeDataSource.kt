package me.sphere.appcore

import kotlinx.coroutines.flow.MutableStateFlow
import me.sphere.appcore.dataSource.DataSource
import me.sphere.appcore.utils.Atomic

internal typealias Loading = DataSource.State.Loading
internal typealias Failed = DataSource.State.Failed
internal typealias Value<T> = DataSource.State.Value<T>

internal class FakeDataSource<Value: Any>(): DataSource<Value>() {
    val flow = MutableStateFlow<State<Value>>(Loading)
    val stubRefreshNow = Atomic<() -> Unit> { error("Unexpected call to `stubRefreshNow`.") }

    override val state: Projection<State<Value>>
        get() = flow.asProjection()

    override fun refreshNow() = stubRefreshNow.value.invoke()
}
