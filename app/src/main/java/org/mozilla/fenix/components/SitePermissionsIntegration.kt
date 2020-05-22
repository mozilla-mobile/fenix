/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Context
import androidx.core.content.getSystemService
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import mozilla.components.feature.sitepermissions.SitePermissionsFeature
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.base.feature.OnNeedToRequestPermissions
import mozilla.components.support.base.feature.PermissionsFeature
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.settings.PhoneFeature
import org.mozilla.fenix.settings.registerOnSharedPreferenceChangeListener
import org.mozilla.fenix.wifi.SitePermissionsWifiIntegration

/**
 * Integrates [SitePermissionsFeature] from Android Components with Fenix's [settings].
 */
class SitePermissionsIntegration(
    context: Context,
    private val lifecycleOwner: LifecycleOwner,
    sessionId: String?,
    fragmentManager: FragmentManager,
    promptsStyling: SitePermissionsFeature.PromptsStyling,
    onNeedToRequestPermissions: OnNeedToRequestPermissions,
    onShouldShowRequestPermissionRationale: (permission: String) -> Boolean
) : LifecycleAwareFeature, PermissionsFeature {

    private val applicationContext = context.applicationContext
    private val settings = context.settings()

    val feature = SitePermissionsFeature(
        context = context,
        sessionManager = context.components.core.sessionManager,
        sessionId = sessionId,
        storage = context.components.core.permissionStorage.permissionsStorage,
        fragmentManager = fragmentManager,
        promptsStyling = promptsStyling,
        onNeedToRequestPermissions = onNeedToRequestPermissions,
        onShouldShowRequestPermissionRationale = onShouldShowRequestPermissionRationale
    )

    override val onNeedToRequestPermissions get() = feature.onNeedToRequestPermissions

    override fun onPermissionsResult(permissions: Array<String>, grantResults: IntArray) =
        feature.onPermissionsResult(permissions, grantResults)

    /**
     * Updates the site permissions rules based on user settings.
     */
    private fun assignSitePermissionsRules() {
        feature.sitePermissionsRules = settings.getSitePermissionsCustomSettingsRules()
    }

    /**
     * Adds a listener that gets called whenever a [PhoneFeature] setting is changed.
     */
    private fun setSitePermissionSettingChangeListener(owner: LifecycleOwner) {
        val sitePermissionKeyToFeature = PhoneFeature.values()
            .map { phoneFeature -> phoneFeature.getPreferenceKey(applicationContext) }
            .toSet()

        settings.preferences.registerOnSharedPreferenceChangeListener(owner) { _, key ->
            if (key in sitePermissionKeyToFeature) assignSitePermissionsRules()
        }
    }

    override fun start() {
        feature.start()
        lifecycleOwner.lifecycle.addObserver(
            SitePermissionsWifiIntegration(
                settings = settings,
                connectivityManager = applicationContext.getSystemService()!!
            )
        )

        setSitePermissionSettingChangeListener(lifecycleOwner)
        assignSitePermissionsRules()
    }

    override fun stop() {
        feature.stop()
    }
}
