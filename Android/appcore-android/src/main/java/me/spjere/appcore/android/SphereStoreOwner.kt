package me.spjere.appcore.android

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import me.sphere.appcore.*
import me.sphere.logging.Logger
import me.sphere.network.ConnectivityMonitor
import me.sphere.sqldelight.StoreActorBuilder
import java.util.concurrent.atomic.AtomicReference

class SphereStoreOwner(
    private val gitHubAccessToken: String,
    private val sqlDatabaseProvider: SqlDatabaseProvider,
    private val storeActorBuilders: List<StoreActorBuilder>,
    private val logger: Logger,
    private val preferenceStore: PreferenceStore,
    private val connectivityMonitor: ConnectivityMonitor
) {

    private val atomicStore = AtomicReference<SphereStore>(null)
    private val storeFlowState = MutableStateFlow<SphereStore?>(null)

    fun acquireStore(): SphereStore {
        return atomicStore.get() ?: createStore().also {
            atomicStore.set(it)
            storeFlowState.value = it
        }
    }

    fun observeStore(): Flow<SphereStore> = storeFlowState.filterNotNull()

    private fun createStore(): SphereStore {
        return SphereStoreBuilder(
            storeActorBuilders,
            sqlDatabaseProvider,
            preferenceStore,
            connectivityMonitor,
            logger,
        ).makeStore(gitHubAccessToken)
    }
}