package me.sphere.appcore.dataSource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import me.sphere.appcore.Projection
import me.sphere.appcore.asProjection
import me.sphere.appcore.utils.freeze
import me.sphere.sqldelight.operations.DeduplicatingInput
import me.sphere.sqldelight.operations.OperationDefinition
import me.sphere.sqldelight.operations.OperationUtils

internal fun <Type: Any> Flow<Type>.asDataSource(startsWithLoading: Boolean = false) = object: DataSource<Type>() {
    init { freeze() }
    override fun refreshNow() {}

    @Suppress("USELESS_CAST")
    override val state: Projection<State<Type>>
        get() = this@asDataSource
            .map { State.Value(it) as State<Type> }
            .catch {
                emit(State.Failed(it))
            }
            .onStart {
                if (startsWithLoading)
                    emit(State.Loading)
            }
            .asProjection()
}

internal fun <Type: Any, Input: DeduplicatingInput> Flow<Type>.asDataSource(
    operationUtils: OperationUtils,
    operation: OperationDefinition<Input, *>,
    input: Input,
    isLocalDataAbsent: () -> Boolean
) = object: DataSource<Type>() {
    override val state: Projection<State<Type>> get() = impl.state.asProjection()

    private val impl = DataSourceImpl(
        operationUtils,
        operation,
        input,
        isLocalDataAbsent
    ) { this@asDataSource }

    init { freeze() }
    override fun refreshNow() = impl.refreshNow()
}
