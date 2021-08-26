package me.sphere.test.support

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import me.sphere.appcore.*

actual object InMemorySqlDatabaseProvider : SqlDatabaseProvider {
    override fun driverForDatabase(name: String, schema: SqlDriver.Schema, didOpen: ((SqlDriver) -> Unit)?): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        schema.create(driver)
        didOpen?.invoke(driver)
        return driver
    }

    override fun destroy(name: String) {
        TODO("Not yet implemented")
    }
}
