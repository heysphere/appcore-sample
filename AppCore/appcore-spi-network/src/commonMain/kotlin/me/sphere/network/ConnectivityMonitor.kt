package me.sphere.network

import kotlinx.coroutines.flow.Flow

interface ConnectivityMonitor {
    fun isNetworkLikelyAvailable(): Flow<Boolean>
}
