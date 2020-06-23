/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import android.content.Intent
import mozilla.components.support.utils.SafeIntent
import mozilla.components.support.utils.toSafeIntent
import org.mozilla.fenix.customtabs.ExternalAppBrowserActivity

/**
 * Tracks how the application was opened through [Event.OpenedAppAllStart]
 * Since OnNewIntent() is always called before onStart() (in a warm/hot startup), we update
 * our current intent with the new intent. However we only track the event during onStart()
 * Because we dont want random intents with [Intent.ACTION_VIEW] to be recorded as app_opened
 * For example, Start app through APP_ICON -> run adb intent command to open a link.
 * We want to ignore these adb commands since app is already on foreground.
 */
class AllSourceStartupTelemetry(private var currentIntent: Intent?, private val metrics: MetricController) {

    /**
     *  Called from activity's onStart() function. records the OpenedAppALlStart telemetry.
     */
    fun onStartHomeActivity() {
        currentIntent?.toSafeIntent()
            ?.let(::getIntentAllStartSource)
            ?.also {
                metrics.track(Event.OpenedAppAllStart(it))
            }
    }

    /**
     * Called from Activity's onNewIntent() function. sets our currentIntent with the latest one
     * received by activity's onNewIntent() function.
     */
    fun onNewIntentHomeActivity(intent: Intent?) {
        currentIntent = intent
    }

    fun getIntentAllStartSource(intent: SafeIntent): Event.OpenedAppAllStart.Source? {
        val callingActivityName: String = intent.unsafe.component?.className.toString()
        return when {
            callingActivityName == ExternalAppBrowserActivity::class.java.name
                -> Event.OpenedAppAllStart.Source.CUSTOM_TAB
            intent.isLauncherIntent -> Event.OpenedAppAllStart.Source.APP_ICON
            intent.action == Intent.ACTION_VIEW -> Event.OpenedAppAllStart.Source.LINK
            else -> Event.OpenedAppAllStart.Source.UNKNOWN
        }
    }
}
