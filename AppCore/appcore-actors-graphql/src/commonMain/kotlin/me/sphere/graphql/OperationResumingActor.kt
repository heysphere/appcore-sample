package me.sphere.graphql

import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import me.sphere.appcore.utils.combinePrevious
import me.sphere.logging.Logger
import me.sphere.network.*
import me.sphere.sqldelight.*

internal class OperationResumingActor(
    private val database: SqlDatabaseGateway,
    private val httpClient: HTTPClient,
    private val logger: Logger,
    storeScope: StoreScope
) : StoreActorBase(storeScope) {

    override fun attach() {
        httpClient
            .isNetworkLikelyAvailable()
            .combinePrevious(false)
            .filter { it == Pair(false, true) }
            .flowOn(StoreScope.ActorListen)
            .onEach {
                database.managedOperationQueries.restartSuspended(Clock.System.now(), StoreScope.clientType)
                logger.info { "Detected network reachability resumption — resuming suspended operations." }
            }
            .launchIn(StoreScope.WriteScope)
    }
}
