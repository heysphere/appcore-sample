package me.sphere.appcore.rest

import me.sphere.appcore.rest.notifications.NotificationReconciliationActor
import me.sphere.logging.Logger
import me.sphere.network.HTTPClient
import me.sphere.sqldelight.StoreScope
import me.sphere.sqldelight.SqlDatabaseGateway
import me.sphere.sqldelight.StoreActor
import me.sphere.sqldelight.StoreActorBuilder

class Backend0StoreActorsBuilder(
    private val httpClient: HTTPClient,
    private val logger: Logger,
): StoreActorBuilder {
    override fun build(database: SqlDatabaseGateway, storeScope: StoreScope): List<StoreActor> = listOfNotNull(
        NotificationReconciliationActor(httpClient, storeScope, database, logger)
    )
}
