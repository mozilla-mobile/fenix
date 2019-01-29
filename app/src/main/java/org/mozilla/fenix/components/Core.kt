/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import mozilla.components.browser.engine.gecko.GeckoEngine
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.session.storage.SessionStorage
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import mozilla.components.concept.engine.DefaultSettings
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession.TrackingProtectionPolicy
import mozilla.components.feature.session.HistoryDelegate
import java.util.concurrent.TimeUnit

/**
 * Component group for all core browser functionality.
 */
class Core(private val context: Context) {

    /**
     * The browser engine component initialized based on the build
     * configuration (see build variants).
     */
    val engine: Engine by lazy {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        val defaultSettings = DefaultSettings(
            remoteDebuggingEnabled = false,
            testingModeEnabled = false,
            trackingProtectionPolicy = createTrackingProtectionPolicy(prefs),
            historyTrackingDelegate = HistoryDelegate(historyStorage)
        )
        GeckoEngine(context, defaultSettings)
    }

    /**
     * The session manager component provides access to a centralized registry of
     * all browser sessions (i.e. tabs). It is initialized here to persist and restore
     * sessions from the [SessionStorage], and with a default session (about:blank) in
     * case all sessions/tabs are closed.
     */
    val sessionManager by lazy {
        val sessionStorage = SessionStorage(context, engine)

        SessionManager(engine, defaultSession = { Session("about:blank") }).apply {
            sessionStorage.restore()?.let { snapshot -> restore(snapshot) }

            if (size == 0) {
                val initialSession = Session("https://www.mozilla.org")
                add(initialSession)
            }

            sessionStorage.autoSave(this)
                .periodicallyInForeground(interval = 30, unit = TimeUnit.SECONDS)
                .whenGoingToBackground()
                .whenSessionsChange()
        }
    }

    /**
     * The storage component to persist browsing history (with the exception of
     * private sessions).
     */
    val historyStorage by lazy { PlacesHistoryStorage(context) }

    /**
     * Constructs a [TrackingProtectionPolicy] based on current preferences.
     *
     * @param prefs the shared preferences to use when reading tracking
     * protection settings.
     * @param normalMode whether or not tracking protection should be enabled
     * in normal browsing mode, defaults to the current preference value.
     * @param privateMode whether or not tracking protection should be enabled
     * in private browsing mode, default to the current preference value.
     * @return the constructed tracking protection policy based on preferences.
     */
    fun createTrackingProtectionPolicy(
        prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context),
        normalMode: Boolean = true,
        privateMode: Boolean = true
    ): TrackingProtectionPolicy {

        return when {
            normalMode && privateMode -> TrackingProtectionPolicy.all()
            normalMode && !privateMode -> TrackingProtectionPolicy.all().forRegularSessionsOnly()
            !normalMode && privateMode -> TrackingProtectionPolicy.all().forPrivateSessionsOnly()
            else -> TrackingProtectionPolicy.none()
        }
    }
}
