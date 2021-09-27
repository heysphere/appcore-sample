package me.spjere.appcore.android.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.annotation.MainThread

/**
 * This class provides an update for the network connectivity. The [Listener] can be called
 * multiple times for the same network.
 */
@SuppressLint("MissingPermission")
class NetworkObserver(
    private val connectivityManager: ConnectivityManager,
) {

    constructor(context: Context) : this(
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager,
    )
    private val listeners = mutableListOf<Listener>()

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = onConnectivityChange(network, true)
            override fun onLost(network: Network) = onConnectivityChange(network, false)
        }
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    /**
     * Registers a new [Listener] that is going to be notified for any network
     * updates
     */
    fun addListener(listener: Listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    /**
     * Removes the provided [Listener]
     */
    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    /** Synchronously checks if the device is online. */
    val isOnline: Boolean
        get() = connectivityManager.allNetworks.any { it.isOnline() }

    private fun onConnectivityChange(network: Network, isOnline: Boolean) {
        val isAnyOnline = connectivityManager.allNetworks.any {
            if (it == network) {
                // Don't trust the network capabilities for the network that just changed.
                isOnline
            } else {
                it.isOnline()
            }
        }
        listeners.forEach { listener ->
            listener.onConnectivityChange(isAnyOnline)
        }
    }

    private fun Network.isOnline(): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(this)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
    }

    /** Calls [onConnectivityChange] when a connectivity change event occurs. */
    fun interface Listener {

        @MainThread
        fun onConnectivityChange(isOnline: Boolean)
    }
}
