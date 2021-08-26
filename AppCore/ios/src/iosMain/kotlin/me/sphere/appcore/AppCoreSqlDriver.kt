package me.sphere.appcore

import com.squareup.sqldelight.Transacter
import com.squareup.sqldelight.db.SqlCursor
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.SqlPreparedStatement
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver
import me.sphere.appcore.utils.autoreleasepool
import me.sphere.appcore.utils.freeze
import platform.Foundation.*
import platform.UIKit.UIBackgroundTaskIdentifier
import platform.UIKit.UIBackgroundTaskInvalid

class AppCoreSqlDriver(
    private val base: NativeSqliteDriver,
    private val taskRunner: BackgroundTaskRunner
): SqlDriver by base {
    init { freeze() }

    override fun newTransaction(): Transacter.Transaction = autoreleasepool {
        // Skip background task wrapping for nested transactions.
        if (base.currentTransaction() != null) {
            return base.newTransaction()
        }

        val taskId = taskRunner.beginTask("appcore-transaction")

        // We should start the background task before creating the transaction, since we will never know if the
        // transaction is DEFERRED or IMMEDIATE.
        return base.newTransaction().apply {
            val endTask = { taskRunner.endTask(taskId) }
            afterCommit(endTask)
            afterRollback(endTask)
        }
    }

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> R,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?
    ): R = autoreleasepool {
        base.executeQuery(identifier, sql, mapper, parameters, binders)
    }

    override fun execute(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?
    ): Unit = autoreleasepool {
        // Skip background task wrapping if the statement is executed in a transaction.
        val taskId = when (base.currentTransaction() != null) {
            false -> taskRunner.beginTask("appcore-dml")
            true -> null
        }

        base.execute(identifier, sql, parameters, binders)

        taskId?.apply(taskRunner::endTask)
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
private fun checkTaskId(id: UIBackgroundTaskIdentifier) {
    if (id == UIBackgroundTaskInvalid) {
        throw BackgroundTaskCreationException()
    }
}

class BackgroundTaskCreationException: RuntimeException("""
    Failed to create a UIKit background task to protect the database write. This could mean the background
    time has been exhausted.
""".trimIndent())
