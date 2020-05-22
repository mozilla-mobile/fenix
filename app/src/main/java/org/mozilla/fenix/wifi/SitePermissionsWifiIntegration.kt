/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.wifi

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Transformations
import androidx.lifecycle.observe
import mozilla.components.feature.sitepermissions.SitePermissionsRules
import org.mozilla.fenix.settings.PhoneFeature
import org.mozilla.fenix.settings.sitepermissions.AUTOPLAY_ALLOW_ON_WIFI
import org.mozilla.fenix.settings.sitepermissions.AUTOPLAY_BLOCK_ALL
import org.mozilla.fenix.utils.Settings

/**
 * Handles implementation details of only setting up a WiFi connectivity listener if the current
 * user settings require it.
 */
class SitePermissionsWifiIntegration(
    private val settings: Settings,
    connectivityManager: ConnectivityManager
) : DefaultLifecycleObserver {

    /**
     * Listener to check when WiFi is available.
     */
    private val wifiAvailable = NetworkOnlineLiveData(
        connectivityManager,
        request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
    )

    /**
     * Builds listener for autoplay setting [AUTOPLAY_ALLOW_ON_WIFI].
     * Sets all autoplay to allowed when WiFi is connected, blocked otherwise.
     */
    private val action = Transformations.map(wifiAvailable) { connected ->
        if (connected) SitePermissionsRules.Action.ALLOWED else SitePermissionsRules.Action.BLOCKED
    }

    private fun updateSettings(action: SitePermissionsRules.Action) {
        settings.setSitePermissionsPhoneFeatureAction(PhoneFeature.AUTOPLAY_AUDIBLE, action)
        settings.setSitePermissionsPhoneFeatureAction(PhoneFeature.AUTOPLAY_INAUDIBLE, action)
    }

    private fun autoplayOnWifi() =
        settings.getAutoplayUserSetting(default = AUTOPLAY_BLOCK_ALL) == AUTOPLAY_ALLOW_ON_WIFI

    /**
     * If autoplay is only enabled on WiFi, sets a WiFi listener to set them accordingly.
     * Otherwise no-op.
     */
    override fun onStart(owner: LifecycleOwner) {
        if (autoplayOnWifi()) {
            action.observe(owner, ::updateSettings)
        }
    }
}
