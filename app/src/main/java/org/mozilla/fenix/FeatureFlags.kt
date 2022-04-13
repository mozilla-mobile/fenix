/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import android.content.Context
import android.os.StrictMode
import mozilla.components.support.locale.LocaleManager
import mozilla.components.support.locale.LocaleManager.getSystemDefault
import org.mozilla.fenix.ext.components

/**
 * A single source for setting feature flags that are mostly based on build type.
 */
object FeatureFlags {
    /**
     * Pull-to-refresh allows you to pull the web content down far enough to have the page to
     * reload.
     */
    val pullToRefreshEnabled = Config.channel.isNightlyOrDebug

    /**
     * Enables the Addresses autofill feature.
     */
    val addressesFeature = Config.channel.isNightlyOrDebug

    /**
     * Enables the "recent" tabs feature in the home screen.
     */
    const val showRecentTabsFeature = true

    /**
     * Enables UI features based on history metadata.
     */
    const val historyMetadataUIFeature = true

    /**
     * Enables the recently saved bookmarks feature in the home screen.
     */
    const val recentBookmarksFeature = true

    /**
     * Identifies and separates the tabs list with a secondary section containing least used tabs.
     */
    const val inactiveTabs = true

    /**
     * Identifies and separates the tabs list with a group containing search term tabs.
     */
    val tabGroupFeature = Config.channel.isNightlyOrDebug

    /**
     * Allows tabs to be dragged around as long as tab groups are disabled
     */
    val tabReorderingFeature = Config.channel.isNightlyOrDebug

    /**
     * Show Pocket recommended stories on home.
     */
    fun isPocketRecommendationsFeatureEnabled(context: Context): Boolean {
        val langTag = LocaleManager.getCurrentLocale(context)
            ?.toLanguageTag() ?: getSystemDefault().toLanguageTag()
        return listOf("en-US", "en-CA").contains(langTag)
    }

    /**
     * Enables showing the homescreen onboarding card.
     */
    const val showHomeOnboarding = false

    /**
     * Enables showing the option to clear site data.
     */
    const val showClearSiteData = true

    /**
     * Enables showing the wallpaper functionality.
     */
    const val showWallpapers = true

    /**
     * Enables the Contile top sites.
     */
    const val contileFeature = true

    /**
     * Enables history improvement features.
     */
    const val historyImprovementFeatures = true

    /**
     * Enables themed wallpapers feature.
     */
    fun isThemedWallpapersFeatureEnabled(context: Context): Boolean {
        return context.components.strictMode.resetAfter(StrictMode.allowThreadDiskReads()) {
            val langTag = LocaleManager.getCurrentLocale(context)
                ?.toLanguageTag() ?: getSystemDefault().toLanguageTag()
            listOf("en-US", "es-US").contains(langTag)
        }
    }

    /**
     * Enables the Task Continuity enhancements.
     */
    val taskContinuityFeature = Config.channel.isDebug

    /**
     * Enables the Unified Search feature.
     */
    val unifiedSearchFeature = Config.channel.isNightlyOrDebug

    /**
     * Enables receiving from the messaging framework.
     */
    val messagingFeature = Config.channel.isNightlyOrDebug
}
