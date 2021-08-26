package me.sphere.graphql

import me.sphere.logging.Logger
import me.sphere.network.AgentHTTPClient
import me.sphere.sqldelight.StoreScope
import me.sphere.sqldelight.SqlDatabaseGateway
import me.sphere.sqldelight.StoreActor
import me.sphere.sqldelight.StoreActorBuilder

class GraphQLStoreActorsBuilder(
    private val httpClient: AgentHTTPClient,
    private val logger: Logger
) : StoreActorBuilder {
    override fun build(database: SqlDatabaseGateway, storeScope: StoreScope): List<StoreActor> = listOf(
        OperationResumingActor(database, httpClient, logger, storeScope),
    )
}
