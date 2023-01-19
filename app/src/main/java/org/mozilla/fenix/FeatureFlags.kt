/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import android.content.Context
import mozilla.components.support.locale.LocaleManager
import mozilla.components.support.locale.LocaleManager.getSystemDefault

/**
 * A single source for setting feature flags that are mostly based on build type.
 */
object FeatureFlags {

    /**
     * Enables custom extension collection feature,
     * This feature does not only depend on this flag. It requires the AMO collection override to
     * be enabled which is behind the Secret Settings.
     * */
    val customExtensionCollectionFeature = Config.channel.isNightlyOrDebug || Config.channel.isBeta

    /**
     * Pull-to-refresh allows you to pull the web content down far enough to have the page to
     * reload.
     */
    val pullToRefreshEnabled = Config.channel.isNightlyOrDebug

    /**
     * Enables the Sync Addresses feature.
     */
    const val syncAddressesFeature = false

    /**
     * Show Pocket recommended stories on home.
     */
    fun isPocketRecommendationsFeatureEnabled(context: Context): Boolean {
        val langTag = LocaleManager.getCurrentLocale(context)
            ?.toLanguageTag() ?: getSystemDefault().toLanguageTag()
        return listOf("en-US", "en-CA").contains(langTag)
    }

    /**
     * Show Pocket sponsored stories in between Pocket recommended stories on home.
     */
    fun isPocketSponsoredStoriesFeatureEnabled(context: Context): Boolean {
        return isPocketRecommendationsFeatureEnabled(context)
    }

    /**
     * Enables the Unified Search feature.
     */
    val unifiedSearchFeature = Config.channel.isNightlyOrDebug

    /**
     * Enables compose on the tabs tray items.
     */
    val composeTabsTray = Config.channel.isDebug

    /**
     * Enables the wallpaper v2 enhancements.
     */
    const val wallpaperV2Enabled = true

    /**
     * Enables the save to PDF feature.
     */
    const val saveToPDF = true

    /**
     * Enables the notification pre permission prompt.
     */
    val notificationPrePermissionPromptEnabled = Config.channel.isNightlyOrDebug

    /**
     * Enables storage maintenance feature.
     *
     * Feature flag tracking: https://github.com/mozilla-mobile/fenix/issues/27759
     * */
    val storageMaintenanceFeature = Config.channel.isNightlyOrDebug || Config.channel.isBeta
}
