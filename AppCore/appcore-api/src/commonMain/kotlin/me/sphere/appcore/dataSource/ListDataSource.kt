package me.sphere.appcore.dataSource

import com.squareup.sqldelight.Query
import kotlinx.coroutines.flow.map
import me.sphere.appcore.Projection
import me.sphere.appcore.asProjection
import me.sphere.appcore.utils.freeze
import me.sphere.sqldelight.listenAll
import me.sphere.sqldelight.operations.DeduplicatingInput
import me.sphere.sqldelight.operations.OperationDefinition
import me.sphere.sqldelight.operations.OperationUtils

internal fun <Type: Any, Input: DeduplicatingInput, QueryResult : Any> listDataSource(
    operationUtils: OperationUtils,
    operation: OperationDefinition<Input, *>,
    input: Input,
    query: Query<QueryResult>,
    mapper: (QueryResult) -> Type
) : ListDataSource<Type> = object: ListDataSource<Type>() {
    private val impl = DataSourceImpl(
        operationUtils,
        operation,
        input,
        isLocalDataAbsent = { query.executeAsList().map(mapper).isEmpty() },
        listenToDatabaseChanges = { query.listenAll().map { it.map(mapper) } }
    )

    init { freeze() }
    override fun refreshNow() = impl.refreshNow()
    override val state: Projection<State<List<Type>>> get() = impl.state.asProjection()
}
