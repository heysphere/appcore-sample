package me.sphere.sqldelight

import com.squareup.sqldelight.EnumColumnAdapter
import com.squareup.sqldelight.db.SqlDriver
import me.sphere.appcore.SqlDatabaseProvider
import me.sphere.appcore.utils.Closeable
import me.sphere.models.*
import me.sphere.models.chat.*
import me.sphere.sqldelight.operations.OperationStatusAdapter
import me.sphere.sqldelight.paging.PagingItem

enum class StoreClientType {
    /** Android/iOS main app process */
    App,

    /** iOS share extension process */
    Share;
}

fun SqlDatabaseProvider.loadOrCreate(name: String, storeClientType: StoreClientType): Pair<SqlDatabaseGateway, Closeable> {
    val driver = driverForDatabase(name, SqlDatabaseGateway.Schema) { driver ->
        onConnectionEstablished(driver, storeClientType)
    }

    return makeQueryInterface(driver) to object: Closeable {
        override fun close() = driver.close()
    }
}

private fun makeQueryInterface(driver: SqlDriver): SqlDatabaseGateway {
    return SqlDatabaseGateway(
        driver = driver,
        PagingItemAdapter = pagingIndexAdapter(),
        EmojiAdapter = emojiAdapter(),
        ManagedOperationAdapter = managedOperationAdapter(),
        NotificationAdapter = notificationAdapter()
    )
}

private fun notificationAdapter() = Notification.Adapter(
    InstantColumnAdapter
)

private fun pagingIndexAdapter() = PagingItem.Adapter(
    lastInSyncAdapter = InstantColumnAdapter
)

private fun emojiAdapter() = Emoji.Adapter(
    idAdapter = StringIdAdapter(::EmojiId)
)

private fun managedOperationAdapter() = ManagedOperation.Adapter(
    statusAdapter = OperationStatusAdapter,
    clientTypeAdapter = EnumColumnAdapter(),
    lastUpdatedAdapter = InstantColumnAdapter
)

/** Maintenance queries that must run on connection start, before any other logic will start using the connection. */
private fun onConnectionEstablished(driver: SqlDriver, clientType: StoreClientType) = makeQueryInterface(driver).run {
}
