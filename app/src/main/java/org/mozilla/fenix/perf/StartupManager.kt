/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.perf

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.annotation.VisibleForTesting
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import mozilla.components.browser.session.SessionManager
import mozilla.components.feature.privatemode.notification.PrivateNotificationFeature
import mozilla.components.support.ktx.android.arch.lifecycle.addObservers
import mozilla.components.support.utils.SafeIntent
import mozilla.components.support.utils.toSafeIntent
import mozilla.components.support.webextensions.WebExtensionPopupFeature
import org.mozilla.fenix.GleanMetrics.Metrics
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.UriOpenedObserver
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager
import org.mozilla.fenix.browser.browsingmode.DefaultBrowsingModeManager
import org.mozilla.fenix.components.metrics.BreadcrumbsRecorder
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.customtabs.ExternalAppBrowserActivity
import org.mozilla.fenix.ext.breadcrumb
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.home.intent.HomeIntentProcessor
import org.mozilla.fenix.home.intent.StartSearchIntentProcessor
import org.mozilla.fenix.session.PrivateNotificationService
import org.mozilla.fenix.theme.DefaultThemeManager
import org.mozilla.fenix.theme.ThemeManager
import java.lang.ref.WeakReference
import kotlin.reflect.KProperty0

class StartupManager {

    @Suppress("TooManyFunctions")
    companion object {
        @OptIn(ExperimentalCoroutinesApi::class)
        @Suppress("LongParameterList", "LongMethod")
        fun activityOnCreate(
            activity: HomeActivity,
            savedInstanceState: Bundle?,
            isVisuallyComplete: Boolean,
            webExtensionPopupFeature: WebExtensionPopupFeature,
            getBreadcrumbMessage: (NavDestination) -> String,
            externalSourceIntentProcessors: KProperty0<List<HomeIntentProcessor>>,
            action: () -> Unit
        ): StartupData {
            var themeManager: ThemeManager?
            var browsingModeManager: BrowsingModeManager?

            activity.components.strictMode.attachListenerToDisablePenaltyDeath(activity.supportFragmentManager)
            // There is disk read violations on some devices such as samsung and pixel for android 9/10
            action()

            // Diagnostic breadcrumb for "Display already aquired" crash:
            // https://github.com/mozilla-mobile/android-components/issues/7960
            activity.breadcrumb(
                message = "onCreate()",
                data = mapOf(
                    "recreated" to (savedInstanceState != null).toString(),
                    "intent" to (activity.intent?.action ?: "null")
                )
            )

            activity.components.publicSuffixList.prefetch()

            val pair = setupThemeAndBrowsingMode(getModeFromIntentOrLastKnown(activity.intent, activity), activity)
            themeManager = pair.first
            browsingModeManager = pair.second

            browsingModeManager.addListener { newMode ->
                themeManager.currentTheme = newMode
            }

            activity.setContentView(R.layout.activity_home)

            val navHost = activity.supportFragmentManager
                                        .findFragmentById(R.id.container) as NavHostFragment
            // Must be after we set the content view
            if (isVisuallyComplete) {
                activity.components.performance.visualCompletenessQueue
                    .attachViewToRunVisualCompletenessQueueLater(
                        WeakReference(activity.findViewById<LinearLayout>(R.id.rootContainer))
                    )
            }

            val sessionObserver = UriOpenedObserver(activity)

            checkPrivateShortcutEntryPoint(activity.intent)
            val privateNotificationObserver = PrivateNotificationFeature(
                activity.applicationContext,
                activity.components.core.store,
                PrivateNotificationService::class
            ).also {
                it.start()
            }

            if (isActivityColdStarted(activity.intent, savedInstanceState)) {
                externalSourceIntentProcessors.get().any {
                    it.process(
                        activity.intent,
                        navHost.navController,
                        activity.intent
                    )
                }
            }

            Performance.processIntentIfPerformanceTest(activity.intent, activity)

            if (activity.settings().isTelemetryEnabled) {
                activity.lifecycle.addObserver(
                    BreadcrumbsRecorder(
                        activity.components.analytics.crashReporter,
                        navHost.navController, getBreadcrumbMessage
                    )
                )

                val safeIntent = activity.intent?.toSafeIntent()
                safeIntent
                    ?.let(activity::getIntentSource)
                    ?.also { activity.components.analytics.metrics.track(Event.OpenedApp(it)) }
                // record on cold startup
                safeIntent
                    ?.let(::getIntentAllSource)
                    ?.also { activity.components.analytics.metrics.track(Event.AppReceivedIntent(it)) }
            }
            activity.supportActionBar?.hide()

            activity.lifecycle.addObservers(
                webExtensionPopupFeature,
                StartupTimeline.homeActivityLifecycleObserver
            )

            if (shouldAddToRecentsScreen(activity.intent)) {
                activity.intent.removeExtra(HomeActivity.START_IN_RECENTS_SCREEN)
                activity.moveTaskToBack(true)
            }

            captureSnapshotTelemetryMetrics(activity)

            startupTelemetryOnCreateCalled(activity.intent.toSafeIntent(), savedInstanceState != null, activity)

            StartupTimeline.onActivityCreateEndHome(activity) // DO NOT MOVE ANYTHING BELOW HERE.

            return StartupData(sessionObserver, privateNotificationObserver, themeManager,
                browsingModeManager)
        }

        private fun setupThemeAndBrowsingMode(
            mode: BrowsingMode,
            activity: Activity
        ): Pair<ThemeManager, BrowsingModeManager> {
            activity.settings().lastKnownMode = mode
            val mBrowsingModeManager = createBrowsingModeManager(mode, activity)
            val mThemeManager = createThemeManager(mBrowsingModeManager, activity)
            mThemeManager.setActivityTheme(activity)
            mThemeManager.applyStatusBarTheme(activity)
            return Pair(mThemeManager, mBrowsingModeManager)
        }

        /**
         * External sources such as 3rd party links and shortcuts use this function to enter
         * private mode directly before the content view is created. Returns the mode set by the intent
         * otherwise falls back to the last known mode.
         */
        private fun getModeFromIntentOrLastKnown(intent: Intent?, context: Context): BrowsingMode {
            intent?.toSafeIntent()?.let {
                if (it.hasExtra(HomeActivity.PRIVATE_BROWSING_MODE)) {
                    val startPrivateMode = it.getBooleanExtra(HomeActivity.PRIVATE_BROWSING_MODE, false)
                    return BrowsingMode.fromBoolean(isPrivate = startPrivateMode)
                }
            }
            return context.settings().lastKnownMode
        }

        private fun checkPrivateShortcutEntryPoint(intent: Intent) {
            if (intent.hasExtra(HomeActivity.OPEN_TO_SEARCH) &&
                (intent.getStringExtra(HomeActivity.OPEN_TO_SEARCH) ==
                        StartSearchIntentProcessor.STATIC_SHORTCUT_NEW_PRIVATE_TAB ||
                        intent.getStringExtra(HomeActivity.OPEN_TO_SEARCH) ==
                        StartSearchIntentProcessor.PRIVATE_BROWSING_PINNED_SHORTCUT)
            ) {
                PrivateNotificationService.isStartedFromPrivateShortcut = true
            }
        }

        @VisibleForTesting
        internal fun isActivityColdStarted(startingIntent: Intent, activityIcicle: Bundle?): Boolean {
            // First time opening this activity in the task.
            // Cold start / start from Recents after back press.
            return activityIcicle == null &&
                    // Activity was restarted from Recents after it was destroyed by Android while in background
                    // in cases of memory pressure / "Don't keep activities".
                    startingIntent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0
        }

        private fun getIntentAllSource(intent: SafeIntent): Event.AppReceivedIntent.Source? {
            return when {
                intent.isLauncherIntent -> Event.AppReceivedIntent.Source.APP_ICON
                intent.action == Intent.ACTION_VIEW -> Event.AppReceivedIntent.Source.LINK
                else -> Event.AppReceivedIntent.Source.UNKNOWN
            }
        }

        /**
         * Determines whether the activity should be pushed to be backstack
         * (i.e., 'minimized' to the recents screen) upon starting.
         * @param intent - The intent that started this activity.
         *                 Is checked for having the 'START_IN_RECENTS_SCREEN'-extra.
         * @return true if the activity should be started and pushed to the recents screen,
         *         false otherwise.
         */
        private fun shouldAddToRecentsScreen(intent: Intent?): Boolean {
            intent?.toSafeIntent()?.let {
                return it.getBooleanExtra(HomeActivity.START_IN_RECENTS_SCREEN, false)
            }
            return false
        }

        private fun captureSnapshotTelemetryMetrics(activity: Activity) = CoroutineScope(Dispatchers.IO).launch {
            // PWA
            val recentlyUsedPwaCount = activity.components.core.webAppShortcutManager.recentlyUsedWebAppsCount(
                activeThresholdMs = HomeActivity.PWA_RECENTLY_USED_THRESHOLD
            )
            if (recentlyUsedPwaCount == 0) {
                Metrics.hasRecentPwas.set(false)
            } else {
                Metrics.hasRecentPwas.set(true)
                // This metric's lifecycle is set to 'application', meaning that it gets reset upon
                // application restart. Combined with the behaviour of the metric type itself (a growing counter),
                // it's important that this metric is only set once per application's lifetime.
                // Otherwise, we're going to over-count.
                Metrics.recentlyUsedPwaCount.add(recentlyUsedPwaCount)
            }
        }

        private fun createBrowsingModeManager(
            initialMode: BrowsingMode,
            activity: Activity
        ): BrowsingModeManager {
            return DefaultBrowsingModeManager(initialMode, activity.components.settings)
        }

        private fun startupTelemetryOnCreateCalled(
            safeIntent: SafeIntent,
            hasSavedInstanceState: Boolean,
            activity: Activity
        ) {
            if (activity is ExternalAppBrowserActivity) {
                activity.components.appStartupTelemetry.onExternalAppBrowserOnCreate(safeIntent, hasSavedInstanceState)
            } else {
                activity.components.appStartupTelemetry.onHomeActivityOnCreate(safeIntent, hasSavedInstanceState)
            }
        }

        private fun createThemeManager(browsingModeManager: BrowsingModeManager, activity: Activity): ThemeManager {
            return DefaultThemeManager(browsingModeManager.mode, activity)
        }
    }
}

data class StartupData(
    var sessionObserve: SessionManager.Observer,
    var privateNotificationService: PrivateNotificationFeature<PrivateNotificationService>,
    var themeManager: ThemeManager,
    var browsingModeManager: BrowsingModeManager
)
