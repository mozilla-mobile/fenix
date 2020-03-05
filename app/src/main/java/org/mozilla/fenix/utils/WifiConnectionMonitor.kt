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
 * Attaches itself to the [Application] and listens for WIFI available/not available events. This
 * allows calling code to set simpler listeners.
 *
 * Example:
 * ```kotlin
 *  app.components.wifiConnectionListener.addOnWifiConnectedChangedListener { isConnected ->
 *    if (isConnected) {
 *      downloadThing()
 *    }
 *  }
 * ```
 */
class WifiConnectionMonitor(app: Application) {
    private val callbacks = mutableSetOf<(Boolean) -> Unit>()

    private var lastKnownStateWasAvailable = false

    init {
        object : ConnectivityManager.NetworkCallback() {
            init {
                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build()

                val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as
                        ConnectivityManager

                cm.registerNetworkCallback(request, this)
            }

            override fun onLost(network: Network?) {
                callbacks.forEach { it(false) }
                lastKnownStateWasAvailable = false
            }

            override fun onAvailable(network: Network?) {
                callbacks.forEach { it(true) }
                lastKnownStateWasAvailable = true
            }
        }
    }

    /**
     * Adds [onWifiChanged] to a list of listeners that will be called whenever WIFI connects or
     * disconnects.
     *
     * If [onWifiChanged] is successfully added (i.e., it is a new listener), it will be immediately
     * called with the last known state.
     */
    fun addOnWifiConnectedChangedListener(onWifiChanged: (Boolean) -> Unit) {
        if (callbacks.add(onWifiChanged)) {
            onWifiChanged(lastKnownStateWasAvailable)
        }
    }

    /**
     * Removes [onWifiChanged] from the list of listeners to be called whenever WIFI connects or
     * disconnects.
     */
    fun removeOnWifiConnectedChangedListener(onWifiChanged: (Boolean) -> Unit) {
        callbacks.remove(onWifiChanged)
    }
}
