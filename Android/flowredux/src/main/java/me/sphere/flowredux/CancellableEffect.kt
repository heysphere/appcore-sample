package me.sphere.flowredux

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val mutex = Mutex()
private val cancellationJobs: MutableMap<Any, MutableSet<Job>> = mutableMapOf()

@ExperimentalCoroutinesApi
@FlowPreview
fun <Action> Effect<Action>.cancellable(id: Any, cancelInFlight: Boolean = false): Effect<Action> =
    Effect(
        channelFlow {
            if (cancelInFlight) {
                mutex.withLock {
                    cancellationJobs[id]?.forEach { it.cancel() }
                    cancellationJobs.remove(id)
                }
            }

            val deferred = async(start = CoroutineStart.LAZY) {
                this@cancellable.flow.collect { send(it) }
            }

            mutex.withLock {
                cancellationJobs.getOrPut(id) { mutableSetOf() }.add(deferred)
            }

            try {
                deferred.start()
                deferred.await()
            } finally {
                mutex.withLock {
                    val jobs = cancellationJobs[id]
                    jobs?.remove(deferred)
                    if (jobs.isNullOrEmpty()) {
                        cancellationJobs.remove(id)
                    }
                }
            }
        }
    )

/**
 * Cancel an Effect that is scheduled or in progress.
 */
fun <State, Action> Result<State, Action>.cancel(
    id: Any,
) = Result(state, effect.cancel(id))

/**
 * Classifies an effect as a cancellable.
 * If you don't use this method, you will not be able to cancel your effect later on.
 */
fun <State, Action> Result<State, Action>.cancellable(
    id: Any,
    cancelInFlight: Boolean = false
) = Result(state, effect.cancellable(id, cancelInFlight))

fun <State, Action> State.cancel(id: Any) = Result(this, emptyEffect<Action>().cancel(id))

// this is fixing recursion issues
@Suppress("unused")
fun <Action> Effect<Action>.cancel(id: Any) = Effect<Action>(
    flow {
        mutex.withLock {
            cancellationJobs[id]?.forEach { it.cancel() }
            cancellationJobs.remove(id)
        }
    }
)
