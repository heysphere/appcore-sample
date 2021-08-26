package me.sphere.test.support

import co.touchlab.sqliter.DatabaseConfiguration
import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.drivers.native.*
import me.sphere.appcore.*
import platform.Foundation.NSRecursiveLock

actual object InMemorySqlDatabaseProvider : SqlDatabaseProvider {
    override fun driverForDatabase(name: String, schema: SqlDriver.Schema, didOpen: ((SqlDriver) -> Unit)?): SqlDriver {
        val configuration = DatabaseConfiguration(
            name = name,
            version = schema.version,
            inMemory = true,
            create = {
                wrapConnection(it) {
                    schema.create(it)
                }
            },
            upgrade = { connection, old, new ->
                wrapConnection(connection) {
                    schema.migrate(it, old, new)
                }
            }
        )

        return NativeSqliteDriver(configuration)
            .apply {
                didOpen?.invoke(this)
            }
            .let(::TestSqliteDriver)
    }

    override fun destroy(name: String) {
        TODO("Not yet implemented")
    }
}

/**
 * A Native [SqlDriver] workaround for the in-memory SQLite database.
 *
 * The MEMORY and OFF journaling mode does not support multi-reader, single-writer concurrency like WAL, so we must
 * serialize all reads, writes and transactions with one lock.
 */
class TestSqliteDriver(private val base: SqlDriver): SqlDriver by base {
    private val lock = NSRecursiveLock()

    override fun newTransaction(): Transacter.Transaction {
        lock.lock()
        return base.newTransaction().also {
            it.afterRollback { lock.unlock() }
            it.afterCommit { lock.unlock() }
        }
    }

    override fun <R> executeQuery(identifier: Int?, sql: String, mapper: (SqlCursor) -> R, parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?): R {
        lock.lock()
        return base.executeQuery(identifier, sql, mapper, parameters, binders)
            .also { lock.unlock() }
    }

    override fun execute(identifier: Int?, sql: String, parameters: Int, binders: (SqlPreparedStatement.() -> Unit)?) {
        lock.lock()
        return base.execute(identifier, sql, parameters, binders)
            .also { lock.unlock() }
    }
}
