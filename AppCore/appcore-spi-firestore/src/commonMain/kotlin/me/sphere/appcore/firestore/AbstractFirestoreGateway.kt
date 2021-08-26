package me.sphere.appcore.firestore

import kotlinx.coroutines.flow.*
import me.sphere.appcore.utils.autoreleasepool
import me.sphere.appcore.utils.flatMapOnce

abstract class AbstractFirestoreGateway {
    private val firestore = MutableStateFlow<Firestore?>(null)

    fun setFirestore(firestore: Firestore?) {
        this.firestore.value = firestore
    }

    protected fun <T> flatMapLatestFirestore(transform: (Firestore) -> Flow<T>): Flow<T> = firestore
        .flatMapLatest {
            autoreleasepool { it?.let(transform) ?: emptyFlow() }
        }

    protected fun <T> flatMapOnceFirestore(transform: (Firestore) -> Flow<T>): Flow<T> = firestore
        .flatMapOnce { autoreleasepool { transform(firestoreOrThrow()) } }

    protected suspend fun firestoreOrThrow() = firestore.firstOrNull() ?: throw NoFirestoreUserException()
}
