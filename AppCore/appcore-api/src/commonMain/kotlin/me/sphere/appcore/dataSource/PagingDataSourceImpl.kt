package me.sphere.appcore.dataSource

import com.squareup.sqldelight.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import me.sphere.appcore.Projection
import me.sphere.appcore.asProjection
import me.sphere.appcore.utils.combinePrevious
import me.sphere.appcore.utils.loop.Loop
import me.sphere.logging.Logger
import me.sphere.network.ConnectivityMonitor
import me.sphere.sqldelight.SqlDatabaseGateway
import me.sphere.sqldelight.listenOneOrNull
import me.sphere.sqldelight.operations.*
import me.sphere.sqldelight.paging.PagingItemQueries
import kotlin.jvm.JvmInline
import kotlin.time.Duration

internal fun <Row: Any, Item: Any> pagingDataSource(
    collectionKey: String,
    scope: CoroutineScope,
    reconciliationOp: PagingReconciliationDefinition,
    database: SqlDatabaseGateway,
    operationUtils: OperationUtils,
    pageSize: Long,
    getItem: (String) -> Query<Row>,
    mapper: (Row) -> Item,
    databaseUpdateTracking: DatabaseUpdateTracking<Row, *, Item, *>? = null,
    logger: Logger,
    connectivityMonitor: ConnectivityMonitor,
    autoRetryInterval: Duration = Duration.seconds(15)
): PagingDataSource<Item> {
    val collectionId = database.transactionWithResult<Long> {
        val idQuery = database.pagingCollectionQueries.idByKey(collectionKey)
        val id = idQuery.executeAsOneOrNull()

        if (id != null) {
            id
        } else {
            database.pagingCollectionQueries.insert(key = collectionKey)
            idQuery.executeAsOne()
        }
    }

    return PagingDataSourceImpl(
        scope,
        reconciliationOp,
        getItem = getItem,
        mapper = mapper,
        collectionId = collectionId,
        database.pagingItemQueries,
        operationUtils,
        pageSize = pageSize,
        databaseUpdateTracking = databaseUpdateTracking,
        logger = logger,
        connectivityMonitor = connectivityMonitor,
        autoRetryInterval = autoRetryInterval
    )
}

internal class DatabaseUpdateTracking<Row: Any, Timestamp: Comparable<Timestamp>, Item: Any, ItemId: Any>(
    val getUpdateHead: Query<Timestamp>,
    val getUpdatedRows: (RowQueryParams<Timestamp>) -> Query<Row>,
    val getItemIdentifier: (Item) -> ItemId,
) {
    /** [startExclusive] < item.timestamp <= [endInclusive] */
    data class RowQueryParams<Timestamp>(
        /** null first */
        val startExclusive: Timestamp?,
        val endInclusive: Timestamp
    )
}

/**
 * [PagingDataSourceImpl] provides a standard support for caching and exposing API collection resources with a pagination
 * access model (both offset and keyset based).
 *
 * ## Core facets
 * The DataSource internal comprises of three main facets:
 *
 * ### Database tracking structures
 * Each unique paginated collection is tracked as a `PagingCollection`, each of which can have 0 or more cached
 * `PagingItem`s. Each `PagingItem` is linked with an item ID, and is always assigned a monotonic integer position.
 *
 * This design is dissimilar to ConversationEventIndex for the chat, particularly because [PagingDataSourceImpl] is a
 * generic solution that:
 * (1) cannot make domain-specific assumptions (e.g., the append-only, monotonic message time order)
 * (2) is designed to support scenarios where API consumers have neither the knowledge of, nor the access to the fieldsets
 *     involved in the sorting and the filtering of resources.
 *
 * ### Remote Reconciliation
 * Every [PagingDataSourceImpl] must be paired with a [PagingReconciliationDefinition] operation, whose backing
 * [PagingReconciliationActor] is responsible for carrying out requests of the API collection resource, merging the
 * resources into the AppCore database appropriately, and update the PagingCollection tracking structures.
 *
 * The reconciliation process is serialized, and works only on one page at a time.
 *
 * #### Failure handling
 * Reconciliation failure of cached pages is gracefully ignored. Only at the point where we are exhausted
 * of cached items, reconciliation failure is explicitly tracked and retried.
 *
 * For example, if the API call for page 1 has failed, `PagingDataSource` will treat the first page of cache items as if
 * they are reconciled. It then moves on to reconcile page 2, and et cetera for all subsequent pages until it runs out
 * of cached full pages.
 *
 * After the cache exhaustion, [PagingDataSourceImpl] needs an authoritative "end-of-collection" answer, which can only
 * come from the backend API. So failure at this stage can no longer be ignored/skipped gracefully.
 *
 * ### Run-ahead Cache Lookup
 * It loads cached pages of items as requested, until the corresponding PagingCollection is exhausted of cached items.
 * The Cache Lookup process is allowed to run ahead of the Remote Reconciliation process, enabling a more fluid user
 * experience which by default avoids waiting for pagination to complete.
 *
 * ## Planned but unimplemented features
 * 1. Time-to-live — Skip remote reconciliation of a page if none of the items has expired TTL.
 * 2. Database live updates — Keep the in-memory working set always up-to-date against the latest database state.
 * 3. Garbage collection — Maintenance tasks to collect orphaned entries.
 */
private class PagingDataSourceImpl<Row: Any, Item: Any>(
    scope: CoroutineScope,
    reconciliationOp: PagingReconciliationDefinition,
    getItem: (String) -> Query<Row>,
    mapper: (Row) -> Item,
    collectionId: Long,
    indexQueries: PagingItemQueries,
    operationUtils: OperationUtils,
    pageSize: Long,
    databaseUpdateTracking: DatabaseUpdateTracking<Row, *, Item, *>?,
    logger: Logger,
    connectivityMonitor: ConnectivityMonitor,
    autoRetryInterval: Duration
): PagingDataSource<Item>() {
    override val state: Projection<PagingState<Item>>
        get() = loop.state
            .map { state ->
                PagingState(
                    items = state.reconciledItems + state.provisionalItems,
                    status = when (state.reconciliationStatus) {
                        is ReconciliationStatus.EndOfCollection ->
                            PagingStatus.END_OF_COLLECTION
                        is ReconciliationStatus.Reconcile ->
                            // Inform UI of the ongoing reconciliation as "loading", only when we ran out of cached
                            // items to serve.
                            if (state.cacheLookupStatus is CacheLookupStatus.Exhausted)
                                PagingStatus.LOADING
                            else
                                PagingStatus.HAS_MORE
                        is ReconciliationStatus.Idle, is ReconciliationStatus.Failed ->
                            PagingStatus.HAS_MORE

                    }
                )
            }
            .asProjection()

    @OptIn(ExperimentalStdlibApi::class, kotlinx.coroutines.FlowPreview::class)
    private val loop = Loop<State<Item>, Event<Item>>(
        parent = scope,
        initial = State(),
        reducer = { current, event ->
            when (event) {
                is Event.Reload -> State()

                is Event.PrefetchNext -> {
                    // Reconciliation has reached the remote end-of-collection. Logically speaking there is no more
                    // cached items we can load from the database, so we can gracefully ignore this PrefetchNext event.
                    if (current.reconciliationStatus is ReconciliationStatus.EndOfCollection)
                        return@Loop current

                    when (current.cacheLookupStatus) {
                        is CacheLookupStatus.Lookup ->
                            current
                        is CacheLookupStatus.Idle ->
                            current.copy(
                                cacheLookupStatus = CacheLookupStatus.Lookup(
                                    start = ViewIndex(current.reconciledItems.size + current.provisionalItems.size)
                                )
                            )
                        is CacheLookupStatus.Exhausted ->
                            if (current.reconciliationStatus !is ReconciliationStatus.Reconcile)
                                current.copy(
                                    reconciliationStatus = ReconciliationStatus.Reconcile(
                                        start = ViewIndex(current.reconciledItems.size)
                                    )
                                )
                            else
                                current
                    }
                }

                /**
                 * Cache lookup runs ahead of reconciliation, and can repeatedly issue [Event.AppendProvisionalItems] until
                 * the local PagingCollection runs out of items.
                 *
                 * These items are appended immediately, though they are considered _provisional_ and are subject to
                 * removal or replacement later, when the Remote Reconciliation process has caught up.
                 */
                is Event.AppendProvisionalItems ->
                    current.copy(
                        provisionalItems = current.provisionalItems + event.provisionalItems,
                        cacheLookupStatus = CacheLookupStatus.Idle,
                        reconciliationStatus = when (current.reconciliationStatus) {
                            ReconciliationStatus.Idle ->
                                ReconciliationStatus.Reconcile(
                                    start = ViewIndex(current.reconciledItems.size)
                                )
                            else ->
                                current.reconciliationStatus
                        }
                    )
                /**
                 * Remote Reconciliation has been done for a page logically starting at the same location as
                 * [State.provisionalItems].
                 */
                is Event.DidReconcileWithRemote -> {
                    val reconciledItems = current.reconciledItems + event.freshItems
                    val provisionalItems = current.provisionalItems.drop(event.freshItems.size)
                    val newReconciliationStatus = if (event.hasMore) {
                        if (provisionalItems.isNotEmpty()) {
                            ReconciliationStatus.Reconcile(start = ViewIndex(reconciledItems.size))
                        } else {
                            ReconciliationStatus.Idle
                        }
                    } else {
                        ReconciliationStatus.EndOfCollection
                    }

                    current.copy(
                        reconciledItems = reconciledItems,
                        provisionalItems = provisionalItems,
                        reconciliationStatus = newReconciliationStatus,
                        cacheLookupStatus = current.cacheLookupStatus
                            .takeUnless { newReconciliationStatus is ReconciliationStatus.EndOfCollection }
                            ?: CacheLookupStatus.Exhausted
                    )
                }

                is Event.DidFailToReconcile -> {
                    if (current.reconciliationStatus is ReconciliationStatus.Reconcile) {
                        val canSkipReconciliation = current.cacheLookupStatus is CacheLookupStatus.Exhausted
                            && pageSize <= current.provisionalItems.size

                        /**
                         * Reconciliation of the page is gracefully skipped in response to a failure, and we will
                         * move on to the next page.
                         *
                         * Exponential backoff or reactive retrying for connectivity issues should be done at a lower
                         * level.
                         * */
                        if (canSkipReconciliation) {
                            val reconciledItems = current.reconciledItems + current.provisionalItems.take(pageSize.toInt())
                            val provisionalItems = current.provisionalItems.drop(pageSize.toInt())

                            current.copy(
                                reconciledItems = reconciledItems,
                                provisionalItems = provisionalItems,
                                reconciliationStatus = ReconciliationStatus.Reconcile(start = ViewIndex(reconciledItems.size))
                                    .takeIf { provisionalItems.isNotEmpty() }
                                    ?: ReconciliationStatus.Failed
                            )
                        } else {
                            current.copy(reconciliationStatus = ReconciliationStatus.Failed)
                        }
                    } else {
                        current
                    }
                }

                is Event.DbUpdateHeadDidChange -> {
                    if (databaseUpdateTracking == null)
                        return@Loop current

                    current.copy(dbUpdateHead = event.head)
                }

                is Event.DidDiscoverUpdatedItems -> {
                    if (databaseUpdateTracking == null)
                        return@Loop current

                    val getId = databaseUpdateTracking.getItemIdentifier

                    current.copy(
                        provisionalItems = current.provisionalItems.map { event.itemsById[getId(it)] ?: it },
                        reconciledItems = current.reconciledItems.map { event.itemsById[getId(it)] ?: it }
                    )
                }
            }
        }
    ) {
        skippingRepeated(mapper = { it.cacheLookupStatus as? CacheLookupStatus.Lookup }) { input, state ->
            flow<Event<Item>> {
                val indices = indexQueries
                    .getForwardPage(collectionId, state.start + input.start, pageSize)
                    .executeAsList()
                val cachedItems = indices.map { getItem(it.itemId).executeAsOne().let(mapper) }
                emit(Event.AppendProvisionalItems(cachedItems))
            }
        }

        skippingRepeated(mapper = { it.reconciliationStatus as? ReconciliationStatus.Reconcile }) { input, state ->
            flow<Event<Item>> {
                val event = runCatching {
                    val output = operationUtils
                        .execute(
                            reconciliationOp,
                            PagingReconciliationDefinition.Input(
                                collectionId = collectionId,
                                start = input.start.index.toLong(),
                                pageSize = pageSize
                            )
                        )
                        .first()

                    val indices = indexQueries
                        .getForwardPage(collectionId, state.start + input.start, pageSize)
                        .executeAsList()
                    val reconciledItems = indices.map { getItem(it.itemId).executeAsOne() }

                    Event.DidReconcileWithRemote(
                        reconciledItems.map(mapper),
                        hasMore = when (output) {
                            PagingReconciliationDefinition.Output.HAS_MORE -> true
                            PagingReconciliationDefinition.Output.END_OF_COLLECTION -> false
                        }
                    )
                }

                event.exceptionOrNull()?.also(logger::exception)
                emit(event.getOrNull() ?: Event.DidFailToReconcile)
            }
        }

        whenBecomesTrue(predicate = { it.reconciliationStatus is ReconciliationStatus.Failed }) { state ->
            flow {
                coroutineScope {
                    /** Whichever comes first: Network becoming available, or 15 seconds timeout */
                    select<Unit> {
                        connectivityMonitor.isNetworkLikelyAvailable()
                            .filter { it }
                            .take(1)
                            .produceIn(this@coroutineScope)
                            .onReceive {}

                        onTimeout(autoRetryInterval) {}
                    }
                    emit(Event.PrefetchNext)
                }
            }
        }

        if (databaseUpdateTracking != null) {
            whenInitialized {
                databaseUpdateTracking.getUpdateHead.listenOneOrNull()
                    .map { Event.DbUpdateHeadDidChange(it) }
            }

            custom { snapshots ->
                snapshots
                    .map { it.state.dbUpdateHead }
                    .distinctUntilChanged()
                    .conflate()
                    .combinePrevious(null)
                    .map { (startExclusive, endInclusive) ->
                        checkNotNull(endInclusive)
                        val params = DatabaseUpdateTracking.RowQueryParams(startExclusive, endInclusive)
                        @Suppress("UNCHECKED_CAST")
                        val query = databaseUpdateTracking.getUpdatedRows as (DatabaseUpdateTracking.RowQueryParams<Comparable<Nothing>>) -> Query<Row>

                        val items = query(params).executeAsList()
                            .map(mapper)
                            .associateBy(databaseUpdateTracking.getItemIdentifier)

                        Event.DidDiscoverUpdatedItems(items)
                    }
            }
        }
    }

    private data class State<Item: Any>(
        val reconciledItems: List<Item> = emptyList(),
        val provisionalItems: List<Item> = emptyList(),
        val reconciliationStatus: ReconciliationStatus = ReconciliationStatus.Idle,
        val cacheLookupStatus: CacheLookupStatus = CacheLookupStatus.Lookup(ViewIndex(0)),

        /**
         * The DB position where this session starts.
         *
         * For now this is constant zero. But this can be non-zero once we start doing compaction to keep a minimal
         * memory footprint.
         */
        val start: DbPosition = DbPosition(0),

        val dbUpdateHead: Comparable<*>? = null,
    )

    private sealed class ReconciliationStatus {
        object Idle: ReconciliationStatus()
        object Failed: ReconciliationStatus()
        object EndOfCollection: ReconciliationStatus()
        data class Reconcile(val start: ViewIndex): ReconciliationStatus()
    }

    private sealed class CacheLookupStatus {
        object Idle: CacheLookupStatus()
        object Exhausted: CacheLookupStatus()
        data class Lookup(val start: ViewIndex): CacheLookupStatus()
    }

    sealed class Event<out Item: Any> {
        object PrefetchNext: Event<Nothing>()
        object Reload: Event<Nothing>()

        class AppendProvisionalItems<Item: Any>(val provisionalItems: List<Item>): Event<Item>()
        class DidReconcileWithRemote<Item: Any>(
            val freshItems: List<Item>,
            val hasMore: Boolean
        ): Event<Item>()
        object DidFailToReconcile: Event<Nothing>()

        class DbUpdateHeadDidChange(val head: Comparable<*>?): Event<Nothing>()
        class DidDiscoverUpdatedItems<Item: Any>(val itemsById: Map<Any, Item>): Event<Item>()
    }

    override fun next() {
        loop.sendUndispatched(Event.PrefetchNext)
    }

    override fun reload() {
        loop.sendUndispatched(Event.Reload)
    }

    override fun close() {
        loop.close()
    }
}

@JvmInline
private value class ViewIndex(val index: Int)

@JvmInline
private value class DbPosition(val position: Long) {
    operator fun plus(index: ViewIndex): Long = position + index.index
}
