// ARMOR-ANDROID-CONTROL - follows the network of the phone so the app can use the address of the house at home and the one for outside away from home.
// Copyright (C) 2026 JuanenRac (Electro Hobby 3D). GPL-3.0-or-later.
package es.electrohobby3d.armor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The place the phone is in and whether it has any network at all; read by the app and by the background watch. */
class NetworkWatcher(context: Context) {
    private val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java)
    private val _place = MutableStateFlow(NetworkPlace.Unknown)
    private val _available = MutableStateFlow(true)
    val place: StateFlow<NetworkPlace> = _place.asStateFlow()
    val available: StateFlow<Boolean> = _available.asStateFlow()
    private var registered = false

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) { publish(capabilities) }
        override fun onLost(network: Network) { _available.value = false; _place.value = NetworkPlace.Unknown }
    }

    private fun publish(capabilities: NetworkCapabilities) {
        _available.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        _place.value = NetworkPlace.of(
            wifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI), ethernet = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET),
            cellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR), vpn = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN),
        )
    }

    /** Starts following; harmless to call twice. */
    @Synchronized fun start() {
        if (registered) return
        runCatching {
            manager.registerDefaultNetworkCallback(callback)
            registered = true
            manager.getNetworkCapabilities(manager.activeNetwork)?.let(::publish)
        }
    }

    @Synchronized fun stop() {
        if (!registered) return
        runCatching { manager.unregisterNetworkCallback(callback) }
        registered = false
    }
}
