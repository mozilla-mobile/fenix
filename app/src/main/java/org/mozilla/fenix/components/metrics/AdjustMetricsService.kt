/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.LogLevel

class AdjustMetricsService(private val application: Application) : MetricsService {
    override fun start() {
        /*
        if ((BuildConfig.ADJUST_TOKEN.isNullOrEmpty())) {
            Log.i(LOGTAG, "No adjust token defined")

            if (Config.channel.isReleased) {
                throw IllegalStateException("No adjust token defined for release build")
            }

            return
        }

         */

        Log.d("Sawyer", "starting adjust!")

        val adjustToken = "ABCDEFGHIJKL"
        val config = AdjustConfig(
            application,
            adjustToken,
            AdjustConfig.ENVIRONMENT_SANDBOX,
            true
        )

        //config.setLogLevel(LogLevel.SUPRESS)

        // TODO: Set on attribute
        config.setOnAttributionChangedListener {
            Log.d("Sawyer", "it changed!: " + it.campaign)
        }

        config.setOnEventTrackingSucceededListener {
            Log.d("Sawyer", "Event success callback called!")
        }



        config.setLogLevel(LogLevel.VERBOSE)
        Adjust.onCreate(config)
        Adjust.setEnabled(true)
        application.registerActivityLifecycleCallbacks(AdjustLifecycleCallbacks())

        Log.d("Sawyer", "attribution: " + Adjust.getAttribution())

    }

    override fun stop() {
        Adjust.setEnabled(false)
    }

    // We're not currently sending events directly to Adjust
    override fun track(event: Event) { /* noop */ }
    override fun shouldTrack(event: Event): Boolean = false

    companion object {
        private const val LOGTAG = "AdjustMetricsService"
    }

    private class AdjustLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            Adjust.onResume()
        }

        override fun onActivityPaused(activity: Activity) {
            Adjust.onPause()
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) { /* noop */ }

        override fun onActivityStarted(activity: Activity) { /* noop */ }

        override fun onActivityStopped(activity: Activity) { /* noop */ }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) { /* noop */ }

        override fun onActivityDestroyed(activity: Activity) { /* noop */ }
    }
}
