package me.sphere.appcore

import kotlinx.coroutines.flow.*
import me.sphere.appcore.utils.*

/**
 * Wrapper of a live updated, immutable projection model.
 *
 * When being subscribed from Swift/ObjC, values and thrown exceptions are always frozen before being passed to
 * Swift/ObjC calls.
 */
open class Projection<State: Any>(base: Flow<State>) : Flow<State> by base {
    init { freeze() }

    fun asSingle(): Single<State> = Single(this)
}

/**
 * Wrapper of a live updated, immutable list of projection models.
 *
 * When being subscribed from Swift/ObjC, values and thrown exceptions are always frozen before being passed to
 * Swift/ObjC calls.
 */
open class ListProjection<State: Any>(base: Flow<List<State>>) : Projection<List<State>>(base)

/**
 * Wrapper of an asynchronous operation.
 *
 * When being subscribed from Swift/ObjC, values and thrown exceptions are always frozen before being passed to
 * Swift/ObjC calls.
 */
class Single<Value: Any>(base: Flow<Value>) : Flow<Value> by base {
    init { freeze() }
}

/**
 * Create a live updated projection that is backed by a `Flow`.
 *
 * Note that a projection requires `State` to be freezable at runtime, and will freeze values upon reception
 * automatically.
 */
fun <State: Any> Flow<State>.asProjection() = Projection(this)

/**
 * Create a live updated projection that is backed by a `Flow`.
 *
 * Note that a projection requires `State` to be freezable at runtime, and will freeze values upon reception
 * automatically.
 */
fun <State: Any> Flow<List<State>>.asListProjection() = ListProjection(this)

/**
 * Create a use case async operation that is backed by a `Single`.
 *
 * Note that a projection requires `State` to be freezable at runtime, and will freeze values upon reception
 * automatically.
 */
fun <State: Any> Flow<State>.asSingle() = Single(this.take(1))
