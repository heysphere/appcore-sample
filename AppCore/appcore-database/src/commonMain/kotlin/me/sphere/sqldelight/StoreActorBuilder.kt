package me.sphere.sqldelight

interface StoreActorBuilder {
    fun build(database: SqlDatabaseGateway, storeScope: StoreScope): List<StoreActor>
}
