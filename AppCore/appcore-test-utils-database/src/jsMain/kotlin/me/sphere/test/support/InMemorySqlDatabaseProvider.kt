package me.sphere.test.support

import me.sphere.appcore.SqlDatabaseProvider
import me.sphere.appcore.utils.Closeable
import me.sphere.sqldelight.SqlDatabaseGateway

actual object InMemorySqlDatabaseProvider : SqlDatabaseProvider {
    override fun driverForDatabase(name: String, schema: SqlDriver.Schema, didOpen: ((SqlDriver) -> Unit)?): SqlDriver {
        TODO("Not yet implemented")
    }

    override fun destroy(name: String) {
        TODO("Not yet implemented")
    }
}
