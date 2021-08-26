package me.sphere.appcore.dataSource

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import me.sphere.appcore.Projection
import me.sphere.appcore.asProjection
import me.sphere.appcore.utils.freeze

fun <T1 : Any, T2 : Any, R : Any, Control> DataSource<T1>.dependant(
    scope: CoroutineScope,
    selector: (T1) -> Control,
    dependent: (Control) -> DataSource<T2>,
    combine: (T1, T2) -> R,
): DataSource<R> = DependentDataSource(this, selector, dependent, combine, scope)

@OptIn(ExperimentalCoroutinesApi::class)
internal class DependentDataSource<T1 : Any, T2 : Any, R : Any, Control>(
    private val source1: DataSource<T1>,
    selector: (T1) -> Control,
    source2: (Control) -> DataSource<T2>,
    combine: (T1, T2) -> R,
    scope: CoroutineScope
) : DataSource<R>() {

    private val sharedSource1 = source1.state.shareIn(scope, WhileSubscribed(0, 0), 1)
    private val dataSourceR2 = sharedSource1
        .mapNotNull { (it as? State.Value<T1>)?.value?.let(selector) }
        .distinctUntilChanged()
        .map { source2(it) }
        .shareIn(scope, WhileSubscribed(0, 0), 1)

    override val state: Projection<State<R>> = sharedSource1
        .combine(dataSourceR2.flatMapLatest { it.state }) { r1, r2 ->
            when (r1) {
                is State.Loading -> State.Loading
                is State.Value -> transform(r1.value, r2, combine)
                is State.Failed -> State.Failed(r1.error)
            }
        }
        .asProjection()

    init { freeze() }

    override fun refreshNow() {
        this.source1.refreshNow()
        this.dataSourceR2.replayCache.firstOrNull()?.refreshNow()
    }
}

private inline fun <T1: Any, T2: Any, R: Any> transform(
    valueR1: T1,
    stateR2: DataSource.State<T2>,
    combine: (T1, T2) -> R
): DataSource.State<R> = when (stateR2) {
    is DataSource.State.Value -> DataSource.State.Value(combine(valueR1, stateR2.value))
    is DataSource.State.Loading -> DataSource.State.Loading
    is DataSource.State.Failed -> DataSource.State.Failed(stateR2.error)
}
