package me.sphere.sqldelight.operations

import kotlinx.coroutines.flow.Flow
import me.sphere.sqldelight.StoreScope
import me.sphere.sqldelight.StoreActorBase

abstract class SubscribeOperationActor<Input, Output>(
    StoreScope: StoreScope
): StoreActorBase(StoreScope) {
    abstract val definition: SubscribeOperationDefinition<Input, Output>

    override fun attach() {}
    abstract fun perform(input: Input): Flow<Output>
}
