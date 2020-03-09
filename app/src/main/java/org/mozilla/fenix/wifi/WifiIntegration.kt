/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.wifi

import mozilla.components.feature.sitepermissions.SitePermissionsRules
import org.mozilla.fenix.settings.PhoneFeature
import org.mozilla.fenix.settings.sitepermissions.AUTOPLAY_ALLOW_ON_WIFI
import org.mozilla.fenix.settings.sitepermissions.AUTOPLAY_BLOCK_ALL
import org.mozilla.fenix.utils.Settings

/**
 * Handles implementation details of only setting up a WIFI connectivity listener if the current
 * user settings require it.
 */
class WifiIntegration(private val settings: Settings, private val wifiConnectionMonitor: WifiConnectionMonitor) {

    /**
     * Adds listener for autplay setting [AUTOPLAY_ALLOW_ON_WIFI]. Sets all autoplay to allowed when
     * WIFI is connected, blocked otherwise.
     */
    private val wifiConnectedListener: ((Boolean) -> Unit) by lazy {
        { connected: Boolean ->
            val setting =
                if (connected) SitePermissionsRules.Action.ALLOWED else SitePermissionsRules.Action.BLOCKED
            settings.setSitePermissionsPhoneFeatureAction(PhoneFeature.AUTOPLAY_AUDIBLE, setting)
            settings.setSitePermissionsPhoneFeatureAction(PhoneFeature.AUTOPLAY_INAUDIBLE, setting)
        }
    }

    /**
     * If autoplay is only enabled on WIFI, sets a WIFI listener to set them accordingly. Otherwise
     * noop.
     */
    fun maybeAddWifiConnectedListener() {
        if (settings.getAutoplayUserSetting(default = AUTOPLAY_BLOCK_ALL) == AUTOPLAY_ALLOW_ON_WIFI) {
            addWifiConnectedListener()
        }
    }

    fun addWifiConnectedListener() {
        wifiConnectionMonitor.addOnWifiConnectedChangedListener(wifiConnectedListener)
    }

    fun removeWifiConnectedListener() {
        wifiConnectionMonitor.removeOnWifiConnectedChangedListener(wifiConnectedListener)
    }

    fun start() { wifiConnectionMonitor.start() }

    fun stop() { wifiConnectionMonitor.stop() }
}
