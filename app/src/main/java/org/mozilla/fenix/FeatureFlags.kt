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
    const val pullToRefreshEnabled = false

    /**
     * Allows edit of saved logins.
     */
    const val loginsEdit = true

    /**
     * Enable tab sync feature
     */
    const val syncedTabs = true

    /**
     * Enables new tab tray pref
     */
    val tabTray = Config.channel.isNightlyOrDebug

    /**
     * Enables gestures on the browser chrome that depend on a [SwipeGestureLayout]
     */
    val browserChromeGestures = Config.channel.isNightlyOrDebug

    /**
     * Enables viewing tab history
     */
    val tabHistory = Config.channel.isNightlyOrDebug
}
