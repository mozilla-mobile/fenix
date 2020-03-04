/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.utils

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.content.Context.WIFI_SERVICE
import androidx.core.content.ContextCompat.getSystemService
import android.net.wifi.WifiManager



/**
 * TODO
 */
class WifiConnectivityMonitor(app: Application) {
    private val callbacks = mutableSetOf(
        OnWifiChanged { lastKnownState = it }
    )

    private var lastKnownState: Boolean

    init {
        // TODO move this setup code into the adapter
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        val adapter = FrameworkAdapter { callbacks }

        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as
                ConnectivityManager

        lastKnownState = adapter.networkIsUnmetered(app)

        cm.registerNetworkCallback(request, adapter)
    }

    /**
     * TODO mention that it calls on add
     */
    fun addOnWifiConnectedChangedListener(onWifiChanged: OnWifiChanged) {
        if (callbacks.add(onWifiChanged)) {
            onWifiChanged(lastKnownState)
        }
    }

    /**
     * TODO
     */
    fun removeOnWifiConnectedChangedListener(onWifiChanged: OnWifiChanged) {
        callbacks.remove(onWifiChanged)
    }
}

/**
 * TODO
 */
private class FrameworkAdapter(private val getCallbacks: () -> Set<OnWifiChanged>) :  ConnectivityManager.NetworkCallback () {
    // TODO listener checks for wifi, `networkIsUnmetered` checks for unmetered.  Decide on one, and be consistent

    override fun onLost(network: Network?) {
        getCallbacks().forEach { it(false) }
    }

    override fun onAvailable(network: Network?) {
        getCallbacks().forEach { it(true) }
    }

    fun networkIsUnmetered(app: Application): Boolean {
        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as
                ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm.getNetworkCapabilities(cm.activeNetwork)
                .hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        } else {
            // TODO not sure this else branch even works
            val wifiManager = app.getSystemService(WIFI_SERVICE) as WifiManager
            val state = WifiInfo
                .getDetailedStateOf(wifiManager.connectionInfo.supplicantState)

            state == NetworkInfo.DetailedState.CONNECTED
        }
    }
}
