package me.sphere.appcore.dataSource

import kotlinx.coroutines.flow.*
import me.sphere.appcore.asProjection
import me.sphere.models.operations.OperationStatus
import me.sphere.sqldelight.operations.DeduplicatingInput
import me.sphere.sqldelight.operations.OperationDefinition
import me.sphere.sqldelight.operations.OperationUtils

/**
 * Common implementation of Data Sources.
 */
internal class DataSourceImpl<Type: Any, Input: DeduplicatingInput>(
    private val operationUtils: OperationUtils,
    private val operation: OperationDefinition<Input, *>,
    private val input: Input,
    private val isLocalDataAbsent: () -> Boolean,
    private val listenToDatabaseChanges: () -> Flow<Type>
) {
    val state: Flow<DataSource.State<Type>>
        get() = flow {
            if (isLocalDataAbsent()) {
                emit((DataSource.State.Loading))
                
                // Execute and wait for `operation` to complete, since we have no local data, and we need
                // the operation to make them available in the DB.
                operationUtils.execute(operation, input).first()
            } else {
                // Enqueue `operation` and proceed without waiting — we can serve the local data immediately.
                // Updates will be eventually delivered via database listening.
                operationUtils.enqueue(operation, input)
            }
            emitAll(listenToDatabaseChanges().map { DataSource.State.Value(it) })
        }.retryWhen { cause, _ ->
            emit(DataSource.State.Failed(cause))
            operationUtils.logger.exception(cause)

            operationUtils.listenStatusOf(operation, input).filter {
                it in listOf(OperationStatus.Idle, OperationStatus.Started, OperationStatus.Success)
            }.first()
            true
        }.asProjection()

    fun refreshNow() {
        operationUtils.enqueue(operation, input)
    }
}
