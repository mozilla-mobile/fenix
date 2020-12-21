/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

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
     * Shows Synced Tabs in the tabs tray.
     *
     * Tracking issue: https://github.com/mozilla-mobile/fenix/issues/13892
     */
    val syncedTabsInTabsTray = Config.channel.isNightlyOrDebug

    /**
     * Enables downloads with external download managers.
     */
    const val externalDownloadManager = true

    /**
     * Enables ETP cookie purging
     */
    val etpCookiePurging = Config.channel.isNightlyOrDebug

    /**
     * Enables the Nimbus experiments library, especially the settings toggle to opt-out of
     * all experiments.
     */
    // IMPORTANT: Only turn this back on once the following issues are resolved:
    // - https://github.com/mozilla-mobile/fenix/issues/17086: Calls to
    // getExperimentBranch seem to block on updateExperiments causing a
    // large performance regression loading the home screen.
    // - https://github.com/mozilla-mobile/fenix/issues/17143: Despite
    // having wrapped getExperimentBranch/withExperiments in a catch-all
    // users are still experiencing crashes.
    const val nimbusExperiments = false

    /**
     * Enables the new MediaSession API.
     */
    val newMediaSessionApi = Config.channel.isNightlyOrDebug

    /**
     * Enabled showing site permission indicators in the toolbars.
     */
    val permissionIndicatorsToolbar = Config.channel.isNightlyOrDebug
}
