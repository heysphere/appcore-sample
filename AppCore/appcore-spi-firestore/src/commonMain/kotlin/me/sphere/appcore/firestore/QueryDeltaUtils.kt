package me.sphere.appcore.firestore

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.DeserializationStrategy
import me.sphere.appcore.utils.Atomic
import me.sphere.appcore.utils.frozenLambda

data class QueryDelta<T>(
    val isInitialDelivery: Boolean,
    val inserted: List<T>,
    val updated: List<T>,
    val removed: Set<String>,
    val decodeFailure: Map<String, Throwable>
) {
    val isNotEmpty: Boolean get() = inserted.isNotEmpty() || updated.isNotEmpty() || removed.isNotEmpty() || decodeFailure.isNotEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
internal inline fun <reified T> CollectionQuery.listenDelta(strategy: DeserializationStrategy<T>): Flow<QueryDelta<T>> = channelFlow {
    val isInitialDelivery = Atomic<Boolean>(true)

    val registration = addSnapshotListenerForAppCore(
        includeMetadataChanges = false,
        frozenLambda { snapshot, throwable ->
            if (throwable == null) {
                check(snapshot != null)

                val inserted = mutableListOf<T>()
                val updated = mutableListOf<T>()
                val removed = mutableSetOf<String>()
                val decodeFailure = mutableMapOf<String, Throwable>()

                for (change in snapshot.documentChanges_) {
                    when (change.type_) {
                        DocumentChangeType.ADDED -> change.addedOrModified(strategy, inserted, decodeFailure)
                        DocumentChangeType.MODIFIED -> change.addedOrModified(strategy, updated, decodeFailure)
                        DocumentChangeType.REMOVED -> removed.add(change.document_.reference_.documentID)
                    }
                }

                val delta = QueryDelta(
                    isInitialDelivery = isInitialDelivery.value,
                    inserted = inserted,
                    updated = updated,
                    removed = removed,
                    decodeFailure = decodeFailure
                )

                if (delta.isNotEmpty) {
                    trySend(delta).also {
                        if (it.isFailure) {
                            throw IllegalStateException("Expected trySend() not to fail with unlimited buffer.", it.exceptionOrNull())
                        }
                    }
                    isInitialDelivery.value = false
                }
            } else {
                close(FirestoreQueryException(T::class, throwable))
            }
        }
    )

    awaitClose(registration::remove)
}.buffer(UNLIMITED)

@PublishedApi
internal inline fun <reified T> DocumentChange.addedOrModified(
    strategy: DeserializationStrategy<T>,
    target: MutableList<T>,
    decodeFailure: MutableMap<String, Throwable>
) {
    val document = document_

    runCatching { firestoreDecode(strategy, document.data()!!, document.reference_) }
        .fold(
            onSuccess = target::add,
            onFailure = { decodeFailure[document.reference_.documentID] = it }
        )
}
