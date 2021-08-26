package me.sphere.sqldelight

import com.squareup.sqldelight.Query
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import me.sphere.appcore.utils.*

private class IdentityDiffingActorState<Identity>(
    val subscriptions: HashMap<Identity, Job> = hashMapOf(),
    var lastReceivedIdentities: List<Identity> = listOf()
)

abstract class IdentityDiffingActorBase<Identity: Any, Output>(StoreScope: StoreScope) : StoreActorBase(StoreScope) {
    // `state` must be accessed only by work on ONE dispatcher on Kotlin/Native.
    private val state = WorkerBound({ IdentityDiffingActorState<Identity>() }.freeze())

    abstract fun listen(): Query<Identity>
    abstract fun onInserted(identity: Identity): Flow<Output>
    abstract fun process(identity: Identity, output: Output)

    final override fun attach() {
        listen()
            .listenAll()
            .onEach(::receiveIdentitiesSnapshot)
            .onCompletion { state.close() }
            .launchIn(StoreScope.ActorListenScope)
    }

    private fun receiveIdentitiesSnapshot(snapshot: List<Identity>) = state.access {
        for (identity in snapshot - lastReceivedIdentities) {
            subscriptions[identity] = onInserted(identity)
                .onEach {
                    process(identity, it)
                }
                .launchIn(StoreScope.WriteScope)
        }

        (lastReceivedIdentities - snapshot)
            .mapNotNull(subscriptions::remove)
            .forEach(Job::cancel)

        lastReceivedIdentities = snapshot
    }
}
