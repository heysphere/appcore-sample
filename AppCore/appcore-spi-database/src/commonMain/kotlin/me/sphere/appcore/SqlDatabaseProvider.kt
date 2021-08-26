package me.sphere.appcore

import com.squareup.sqldelight.db.SqlDriver

interface SqlDatabaseProvider {
    /**
     * Initialize the SQL driver for the database of the given name.
     */
    fun driverForDatabase(
        name: String,
        schema: SqlDriver.Schema,
        didOpen: ((SqlDriver) -> Unit)? = null
    ): SqlDriver

    fun destroy(name: String)

    companion object {}
}

