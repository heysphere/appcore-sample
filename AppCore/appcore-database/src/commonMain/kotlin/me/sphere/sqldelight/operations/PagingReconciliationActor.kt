package me.sphere.sqldelight.operations

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import me.sphere.logging.Logger
import me.sphere.sqldelight.StoreScope
import me.sphere.sqldelight.SqlDatabaseGateway
import me.sphere.sqldelight.paging.PagingItem
import kotlin.jvm.JvmInline

abstract class PagingReconciliationActor<Payload>(
    database: SqlDatabaseGateway,
    logger: Logger,
    storeScope: StoreScope,
    protected val clock: Clock = Clock.System
): OperationStoreActorBase<PagingReconciliationDefinition.Input<Payload>, PagingReconciliationDefinition.Output>(database, logger, storeScope) {
    //region Data types
    @JvmInline
    value class ID(val value: String)

    class FetchContext(val start: Long, val after: ID?, val pageSize: Long)
    sealed class FetchResult {
        class Success(val page: List<ID>): FetchResult()
    }
    //endregion

    //region Subclass contract
    abstract override val definition: PagingReconciliationDefinition<Payload>

    abstract suspend fun <Payload> fetch(context: FetchContext, payload: Payload): FetchResult
    //endregion

    override suspend fun perform(input: PagingReconciliationDefinition.Input<Payload>): PagingReconciliationDefinition.Output {
        require(input.start >= 0) { "`start` cannot be negative." }

        val idBeforeStart = when (input.start > 0) {
            true -> database.pagingItemQueries
                .itemAtPosition(collectionId = input.collectionId, position = input.start - 1)
                .executeAsOneOrNull()
                ?.itemId
                ?: error("Collection ${input.collectionId} does not have item at ${input.start -1}.")
            false -> null
        }

        val context = FetchContext(input.start, idBeforeStart?.let(::ID), input.pageSize)

        return when (val result = fetch(context, input.payload)) {
            is FetchResult.Success -> {
                // By default, assume end of collection when the resulting page is smaller than the page size.
                // Could make this customizable when needed in the future.
                val isEndOfCollection = result.page.size < input.pageSize

                database.transaction {
                    val now = clock.now()
                    val queries = database.pagingItemQueries

                    result.page.forEachIndexed { index, id ->
                        val item = PagingItem(
                            collectionId = input.collectionId,
                            position = input.start + index,
                            itemId = id.value,
                            lastInSync = now
                        )
                        queries.add(item)
                    }

                    if (isEndOfCollection) {
                        queries.deleteOnOrAfter(
                            start = input.start + result.page.size,
                            collectionId = input.collectionId
                        )
                    }
                }

                if (isEndOfCollection)
                    PagingReconciliationDefinition.Output.END_OF_COLLECTION
                else
                    PagingReconciliationDefinition.Output.HAS_MORE
            }
        }
    }
}
