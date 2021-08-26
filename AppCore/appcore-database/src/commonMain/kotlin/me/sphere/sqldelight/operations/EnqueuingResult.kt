package me.sphere.sqldelight.operations

import me.sphere.models.operations.OperationId

sealed class EnqueuingResult<out Output> {
    /**
     * The operation has been enqueued.
     *
     * It may be awaiting to be picked up from the queue by an Operation Store Actor, or may potentially be running if
     * the operation has deduplication-by-key enabled.
     */
    data class Enqueued(val id: OperationId): EnqueuingResult<Nothing>()

    /**
     * The operation has an immediate output provided by its fast path. The enqueue is therefore skipped.
     */
    data class Completed<Output>(val output: Output): EnqueuingResult<Output>()
}
