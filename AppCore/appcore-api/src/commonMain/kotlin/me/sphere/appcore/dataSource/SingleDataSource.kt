package me.sphere.appcore.dataSource

import com.squareup.sqldelight.Query
import kotlinx.coroutines.flow.map
import me.sphere.appcore.Projection
import me.sphere.appcore.asProjection
import me.sphere.appcore.utils.freeze
import me.sphere.sqldelight.listenOne
import me.sphere.sqldelight.operations.DeduplicatingInput
import me.sphere.sqldelight.operations.OperationDefinition
import me.sphere.sqldelight.operations.OperationUtils

internal fun <Type: Any, Input: DeduplicatingInput, QueryResult : Any> singleDataSource(
    operationUtils: OperationUtils,
    operation: OperationDefinition<Input, *>,
    input: Input,
    query: Query<QueryResult>,
    mapper: (QueryResult) -> Type
) = object: DataSource<Type>() {
    private val impl = DataSourceImpl(
        operationUtils,
        operation,
        input,
        isLocalDataAbsent = { query.execute { !it.next() } },
        listenToDatabaseChanges = { query.listenOne().map { mapper(it) } }
    )

    init { freeze() }
    override fun refreshNow() = impl.refreshNow()
    override val state: Projection<State<Type>> get() = impl.state.asProjection()
}
