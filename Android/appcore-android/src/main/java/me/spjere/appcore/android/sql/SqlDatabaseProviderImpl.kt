package me.spjere.appcore.android.sql

import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import me.sphere.appcore.SqlDatabaseProvider
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import me.sphere.sqldelight.SQLCORE_SCHEMA_HASH

class SqlDatabaseProviderImpl(private val context: Context) : SqlDatabaseProvider {

    override fun driverForDatabase(name: String, schema: SqlDriver.Schema, didOpen: ((SqlDriver) -> Unit)?): SqlDriver {
        val databaseName = createDatabaseName(identifier = name)
        removeStaleDatabases(databaseName)

        val sqliteFactory = RequerySQLiteOpenHelperFactory()
        // Note: To enable Database inspector remove the sqliteFactory from the constructor call.
        return AndroidSqliteDriver(schema, context, databaseName, sqliteFactory).also {
            didOpen?.invoke(it)
        }
    }

    private fun removeStaleDatabases(databaseName: String) {
        // find all databases except the current one and the ones which belongs to our group
        context.databaseList().filter { it.startsWith(DB_PREFIX) && it != databaseName }.forEach {
            // AOSP is catching here so there is no reason to worry
            // https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/app/ContextImpl.java#894
            context.deleteDatabase(it)
        }
    }

    private fun createDatabaseName(identifier: String): String {
        return "${DB_PREFIX}_${identifier}_$SQLCORE_SCHEMA_HASH"
    }

    override fun destroy(name: String) {
        val databaseName = createDatabaseName(identifier = name)
        context.deleteDatabase(databaseName)
    }

    companion object {
        // We need to separate the appcore db with the rests of the databases
        const val DB_PREFIX = "sphere_db"
    }
}