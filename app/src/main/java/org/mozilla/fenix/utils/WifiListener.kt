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

/**
 * TODO
 */
class WifiConnectivityMonitor(app: Application) {

    private val callbacks = mutableSetOf(
        OnWifiChanged { lastKnownState = it }
    )

    private var lastKnownState: Boolean? = null

    init {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        val adapter = FrameworkAdapter { callbacks }

        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as
                ConnectivityManager
        cm.registerNetworkCallback(request, adapter)
    }

    // TODO make sure the permission listener gets added during app init
    /**
     * TODO mention that it calls on add
     */
    fun addOnWifiConnectedChangedListener(onWifiChanged: OnWifiChanged) {
        lastKnownState?.let { onWifiChanged(it) }
        callbacks.add(onWifiChanged)
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

    override fun onLost(network: Network?) {
        getCallbacks().forEach { it(false) }
    }

    override fun onAvailable(network: Network?) {
        getCallbacks().forEach { it(true) }
    }
}
