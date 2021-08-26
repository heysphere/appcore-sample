package me.sphere.test

import kotlinx.coroutines.flow.*
import me.sphere.appcore.utils.Atomic
import me.sphere.sqldelight.operations.*
import me.sphere.test.support.TestLogger
import kotlin.test.*

abstract class OperationActorTest<Actor: OperationStoreActorBase<Input, Output>, Input, Output>: DbTests() {
    class Scope<Actor>(val actor: Actor, val operationUtils: OperationUtils)

    abstract fun DbTests.Scope.makeActor(): Actor

    fun runActorTesting(body: suspend Scope<Actor>.() -> Unit) = runTesting {
        val actor = this.makeActor()
        storeScope.register(listOf(actor))
        actor.attach()

        Scope(actor, OperationUtils(database, TestLogger, storeScope))
            .body()
    }

    fun Scope<Actor>.testExecuteFastPath(input: Input): Output
        = when (val result = operationUtils.enqueue(actor.definition, input)) {
            is EnqueuingResult.Completed ->
                result.output
            is EnqueuingResult.Enqueued ->
                fail("Expected to receive a fast path output. Got none, and the operation is now enqueued.")
        }

    suspend fun Scope<Actor>.testExecute(input: Input): Output {
        val id = when (val result = operationUtils.enqueue(actor.definition, input)) {
            is EnqueuingResult.Completed ->
                fail("The store has produced an immediate early output, which results in no enqueuing.")
            is EnqueuingResult.Enqueued ->
                result.id
        }

        return operationUtils
            .listen(actor.definition, id)
            .first()
    }
}
