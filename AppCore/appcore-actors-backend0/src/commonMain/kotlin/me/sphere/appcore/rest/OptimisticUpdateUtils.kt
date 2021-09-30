package me.sphere.appcore.rest

import me.sphere.models.chat.ActionableMessagePropsOwner
import me.sphere.models.chat.MessageActionLocalStatus
import me.sphere.sqldelight.SqlDatabaseGateway
import me.sphere.sqldelight.operations.NeedsSuspensionException
import me.sphere.sqldelight.operations.isConnectivityError
import me.sphere.sqldelight.operations.chat.MessagePath
import kotlin.experimental.ExperimentalTypeInference

/**
 * All these effects are automatically wrapped with a database write transaction:
 * - [OptimisticActionBuilder.optimisticallyApply]
 * - [OptimisticActionBuilder.onCallSuccess]
 * - [OptimisticActionBuilder.onCallFailure]
 */
@OptIn(ExperimentalTypeInference::class)
internal suspend inline fun <reified T> SqlDatabaseGateway.optimisticAction(@BuilderInference builder: OptimisticActionBuilder<T>.() -> Unit) =
    OptimisticActionBuilder<T>(this).apply { builder() }.run()

internal class OptimisticActionBuilder<CallResult>(private val database: SqlDatabaseGateway) {

    private var beforeCall: (() -> Unit)? = null
    private lateinit var call: (suspend () -> CallResult)
    private var onCallSuccess: ((CallResult) -> Unit)? = null
    private var onCallFailure: ((Throwable) -> Unit)? = null
    private var onCallSuspended: (() -> Unit)? = null

    fun optimisticallyApply(action: () -> Unit) {
        this.beforeCall = action
    }

    fun runAsync(action: suspend () -> CallResult) {
        this.call = action
    }

    fun onSuccess(action: (CallResult) -> Unit) {
        this.onCallSuccess = action
    }

    fun onFailure(action: (Throwable) -> Unit) {
        this.onCallFailure = action
    }

    suspend fun run() {
        beforeCall?.let { block -> database.transaction { block() } }
        try {
            val result = call.invoke()
            onCallSuccess?.let { block -> database.transaction { block(result) } }
        } catch (error: Throwable) {
            onCallFailure?.let { block -> database.transaction { block(error) } }
            throw error
        }
    }
}
