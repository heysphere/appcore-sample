package me.sphere.appcore.firestore

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import kotlinx.serialization.DeserializationStrategy
import me.sphere.appcore.firestore.FirestoreException.Reason
import me.sphere.appcore.utils.*
import kotlin.reflect.KClass

interface Firestore {
    fun collection(id: String): CollectionReference
}

interface CollectionReference: CollectionQuery {
    fun document(id: String): DocumentReference
}

interface CollectionQuery {
    fun limit(first: Int): CollectionQuery
    fun limitLast(count: Int): CollectionQuery
    fun orderBy(fieldName: String, direction: OrderByDirection): CollectionQuery

    fun startAfter(timestamp: Instant): CollectionQuery
    fun endBefore(timestamp: Instant): CollectionQuery
    fun endAt(timestamp: Instant): CollectionQuery

    fun whereEqualTo(field: String, value: Any): CollectionQuery
    fun whereLessThanOrEqualTo(field: String, value: Any): CollectionQuery

    fun addSnapshotListenerForAppCore(includeMetadataChanges: Boolean, action: (QuerySnapshot?, FirestoreException?) -> Unit): ListenerRegistration
    fun get(action: (List<DocumentSnapshot>, FirestoreException?) -> Unit)

    enum class OrderByDirection {
        Ascending, Descending
    }
}

interface DocumentReference {
    val documentID: String
    val path: String

    fun collection(id: String): CollectionReference

    fun addSnapshotListenerForAppCore(includeMetadataChanges: Boolean, action: (DocumentSnapshot?, FirestoreException?) -> Unit): ListenerRegistration
    fun get(action: (DocumentSnapshot?, FirestoreException?) -> Unit)
}

// NOTE: underscore suffix is added to avoid Objective-C name clashes.

interface DocumentSnapshot {
    @Suppress("PropertyName")
    val reference_: DocumentReference

    @Suppress("PropertyName")
    val metadata_: SnapshotMetadata

    fun data(): Map<String, Any>?
}

interface QuerySnapshot {
    @Suppress("PropertyName")
    val metadata_: SnapshotMetadata

    @Suppress("PropertyName")
    val documents_: List<DocumentSnapshot>

    @Suppress("PropertyName")
    val documentChanges_: List<DocumentChange>
}

interface DocumentChange {
    val type_: DocumentChangeType

    @Suppress("PropertyName")
    val document_: DocumentSnapshot
}

enum class DocumentChangeType {
    ADDED, MODIFIED, REMOVED
}

interface SnapshotMetadata {
    val isFromCache: Boolean
}

interface ListenerRegistration {
    fun remove()
}

class DocumentDoesNotExistException(reference: DocumentReference)
    : RuntimeException("Document does not exist at ${reference.path}.")
class FirestoreDecodeException(reference: DocumentReference, expectedType: KClass<*>, cause: Throwable? = null)
    : RuntimeException("Failed to decode document at ${reference.path} as ${expectedType.simpleName}.", cause)
class FirestoreQueryException(type: String, path: String, expectedType: String, cause: Throwable?)
    : RuntimeException("Failed to query document at $path for $type, to be decoded as $expectedType.", cause)
{
    constructor(reference: DocumentReference, expectedType: KClass<*>, cause: Throwable?)
        : this("document", reference.path, "${expectedType.simpleName}", cause)
    constructor(expectedType: KClass<*>, cause: Throwable?)
        : this("collection", "<unknown>", "List<${expectedType.simpleName}>", cause)
}

open class FirestoreException(
    val reason: Reason = Reason.UNKNOWN,
    message: String,
    cause: Throwable? = null
) : RuntimeException("$message (reason=$reason)", cause) {
    enum class Reason {
        UNAVAILABLE, PERMISSION_DENIED, UNKNOWN;
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
inline fun <reified T> DocumentReference.listen(strategy: DeserializationStrategy<T>) = channelFlow {
    val registration = addSnapshotListenerForAppCore(
        includeMetadataChanges = true,
        frozenLambda { snapshot, throwable ->
            if (throwable == null) {
                check(snapshot != null)

                if (snapshot.metadata_.isFromCache) {
                    // Ignore any snapshot delivered by the cache.
                    return@frozenLambda
                }

                val data = snapshot.data()

                // We are observing a single document, and apparently Firestore sends out a prime `DocumentSnapshot`
                // with no data, when the listener is registered offline. So we need to specifically ignore that prime
                // `null`.
                //
                // "If there is no document at the location referenced by docRef, the resulting document will be empty
                // and calling exists on it will return false."
                // - https://firebase.google.com/docs/firestore/query-data/get-data#get_a_document
                if (data != null) {
                    try {
                        val value: T = firestoreDecode(strategy, data, snapshot.reference_).freeze()
                        offer(value)
                    } catch (e: Throwable) {
                        close(e)   
                    }
                }
            } else {
                close(FirestoreQueryException(this@listen, T::class, throwable))
            }
        }
    )

    awaitClose(registration::remove)
}.firestoreExponentialRetry()

@OptIn(ExperimentalCoroutinesApi::class)
inline fun <reified T> CollectionQuery.listen(strategy: DeserializationStrategy<T>) = channelFlow {
    val registration = addSnapshotListenerForAppCore(
        includeMetadataChanges = true,
        frozenLambda { snapshot, throwable ->
            if (throwable == null) {
                check(snapshot != null)

                if (snapshot.metadata_.isFromCache) {
                    // Ignore any snapshot delivered by the cache.
                    return@frozenLambda
                }

                // "Since query results contain only existing documents, the exists() method will always return true and
                // getData() will never be null."
                //
                // - https://firebase.google.com/docs/reference/android/com/google/firebase/firestore/QueryDocumentSnapshot
                //
                // "As deleted documents are not returned from queries, its exists property will always be true and
                // data: will never return nil."
                //
                // - https://firebase.google.com/docs/reference/swift/firebasefirestore/api/reference/Classes/QueryDocumentSnapshot
                try {
                    val value: List<T> = snapshot.documents_
                        .map { firestoreDecode(strategy, it.data()!!, it.reference_) }
                        .freeze()
                    offer(value)
                } catch (e: Throwable) {
                    close(e)
                }
            } else {
                close(FirestoreQueryException(T::class, throwable))
            }
        }
    )

    awaitClose(registration::remove)
}.firestoreExponentialRetry()

@OptIn(ExperimentalCoroutinesApi::class)
inline fun <reified T> DocumentReference.get(strategy: DeserializationStrategy<T>): Flow<T> = callbackFlow<T> {
    get(
        frozenLambda { snapshot, throwable ->
            if (throwable == null) {
                try {
                    check(snapshot != null)

                    // "If there is no document at the location referenced by docRef, the resulting document will be empty
                    // and calling exists on it will return false."
                    // - https://firebase.google.com/docs/firestore/query-data/get-data#get_a_document
                    val data = snapshot.data() ?: throw DocumentDoesNotExistException(snapshot.reference_)
                    val value: T = firestoreDecode(strategy, data, snapshot.reference_).freeze()

                    offer(value)
                    close()
                } catch (e: Throwable) {
                    close(e)
                }
            } else {
                close(FirestoreQueryException(this@get, T::class, throwable))
            }
        }
    )

    awaitClose()
}.firestoreExponentialRetry()

@OptIn(ExperimentalCoroutinesApi::class)
inline fun <reified T> CollectionQuery.get(strategy: DeserializationStrategy<T>) = callbackFlow {
    get(
        frozenLambda { listOfMap, throwable ->
            if (throwable == null) {
                // "Since query results contain only existing documents, the exists() method will always return true and
                // getData() will never be null."
                //
                // - https://firebase.google.com/docs/reference/android/com/google/firebase/firestore/QueryDocumentSnapshot
                //
                // "As deleted documents are not returned from queries, its exists property will always be true and
                // data: will never return nil."
                //
                // - https://firebase.google.com/docs/reference/swift/firebasefirestore/api/reference/Classes/QueryDocumentSnapshot
                try {
                    val value: List<T> = listOfMap
                        .map { firestoreDecode(strategy, it.data()!!, it.reference_) }
                        .freeze()
                    offer(value)
                    close()
                } catch (e: Throwable) {
                    close(e)
                }
            } else {
                close(FirestoreQueryException(T::class, throwable))
            }
        }
    )

    awaitClose()
}.firestoreExponentialRetry()

@PublishedApi
internal fun <T> Flow<T>.firestoreExponentialRetry(): Flow<T> = exponentialBackoffRetry(
    shouldRetry = { error ->
        (error.cause as? FirestoreException)
            ?.let { it.reason == Reason.PERMISSION_DENIED || it.reason == Reason.UNAVAILABLE }
            ?: false
    }
)
