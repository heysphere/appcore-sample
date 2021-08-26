package me.sphere.appcore.firestore

import me.sphere.logging.Logger
import me.sphere.sqldelight.StoreScope
import me.sphere.sqldelight.SqlDatabaseGateway
import me.sphere.sqldelight.StoreActor
import me.sphere.sqldelight.StoreActorBuilder

class Backend0StoreActorsBuilder(
    private val logger: Logger
): StoreActorBuilder {
    override fun build(database: SqlDatabaseGateway, storeScope: StoreScope): List<StoreActor> = listOfNotNull(
    )
}
