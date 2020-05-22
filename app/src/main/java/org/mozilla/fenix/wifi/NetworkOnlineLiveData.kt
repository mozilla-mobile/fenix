package org.mozilla.fenix.wifi

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import androidx.lifecycle.LiveData

/**
 * [LiveData] that emits network available/not available events.
 * This allows calling code to set simpler listeners.
 *
 * @sample
 * ```kotlin
 *  NetworkOnlineLiveData().observer(owner) { isConnected ->
 *    if (isConnected) {
 *      downloadThing()
 *    }
 *  }
 * ```
 */
class NetworkOnlineLiveData(
    private val connectivityManager: ConnectivityManager,
    private val request: NetworkRequest,
    initialValue: Boolean = false
) : LiveData<Boolean>(initialValue) {

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = postValue(true)
        override fun onLost(network: Network) = postValue(false)
    }

    override fun onActive() {
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    override fun onInactive() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}
