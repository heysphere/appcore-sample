package me.sphere.sqldelight.operations

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Each [OperationDefinitionBase] is a written API contract of an AppCore Operation.
 *
 * At minimum, each operation defines an input payload type and an output payload type. Each input payload creates an
 * instance of the operation when enqueued to the SQLCore infrastructure, from which the owning actor of the
 * operation dequeues and evaluates the instances.
 *
 * There are two subtypes of operations:
 * (1) [OperationDefinition]: produce exactly one output, or fail with an exception.
 * (2) [SubscribeOperationDefinition]: produce zero or more outputs over time, or fail with an exception.
 */
interface OperationDefinitionBase<Input, Output>

/**
 * Represent an AppCore Operation that produces EXACTLY ONE `Output` or fail with an exception. The operation may
 * potentially be suspended for later resumption **across application invocations** upon network unavailability, and
 * hence serializable payload types are required.
 * 
 * The definition must provide a [KSerializer] for both `Input` and `Output` payload types.
 */
interface OperationDefinition<Input, Output>: OperationDefinitionBase<Input, Output> {
    /**
     * A unique identifier of this operation definition.
     *
     * It is recommended to use a string literal, instead of `KClass::simpleName` or `KClass::qualifiedName`.
     */
    val identifier: String

    /**
     * The serializer for the Input payload.
     */
    val inputSerializer: KSerializer<Input>

    /**
     * The serializer for the Output payload.
     */
    val outputSerializer: KSerializer<Output>
}

/**
 * Represent an AppCore Operation that is a long-running routine created on demand, emitting zero or more `Output`s over
 * time.
 */
interface SubscribeOperationDefinition<Input, Output>: OperationDefinitionBase<Input, Output>

/**
 * An operation input payload that should be deduplicated before being enqueued. If present, AppCore deduplicates active
 * instances of the same operation type — that shares the same deduplication key — down to one running instance.
 *
 * You should supply only the static/invariant components of your input parameters as the deduplication key. For
 * example, say an operation is to set a boolean flag on a resource, given a resource ID and the new flag value.
 *
 * (i) If the expectation is that only one should be run at a time, only the resource ID should be used as the
 *     deduplication key.
 * (ii) If the expectation is that they can run as many as they can, it is unnecessary to implement [DeduplicatingInput],
 *      as AppCore recognizes every enqueue of a vanilla operation as unique.
 */
interface DeduplicatingInput {
    val deduplicationKey: String
}

/**
 * Generate a unique key which makes the whole input payload to be considered for deduplication.
 */
inline fun <reified T: DeduplicatingInput> T.wholeValueAsKey(serializer: KSerializer<T>): String
    = Json.encodeToString(serializer, this)

/**
 * A sentinel denoting that the operation requires no input parameter. Unlike [Unit], this implements
 * [DeduplicatingInput] so active instances are deduplicated.
 */
@Serializable
object EmptyInput: DeduplicatingInput {
    override val deduplicationKey: String get() = "singleton"
}
