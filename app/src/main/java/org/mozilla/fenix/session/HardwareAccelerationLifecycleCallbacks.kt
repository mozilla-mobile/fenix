/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.session

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

/**
 * This ActivityLifecycleCallbacks implementation ensures that hardware acceleration is enabled for
 * a Window when an Activity is created. On some devices we cannot enable UI hardware acceleration
 * due to graphics driver issues. So we must globally disable hardware acceleration in
 * AndroidManifest.xml, then enable it where possible on a per-window basis.
 * See https://bugzilla.mozilla.org/show_bug.cgi?id=1609191
 */
@SuppressWarnings("EmptyFunctionBlock")
class HardwareAccelerationLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
    val supportsHardwareAcceleration by lazy {
        // List of boards on which we should keep hardware acceleration disabled.
        // These were the boards with the highest number of crashes caused by hardware acceleration being enabled.
        // There will be some affected devices with different boards which therefore may still crash,
        // however the numbers should be low enough and the crashes infrequent enough for that to be okay.
        val buggyBoards = listOf("msm8953", "msm8976", "msm8937", "msm8996", "sdm450_mh4x")

        Build.BOARD !in buggyBoards
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (supportsHardwareAcceleration) {
            activity.window.setFlags(FLAG_HARDWARE_ACCELERATED, FLAG_HARDWARE_ACCELERATED)
        }
    }

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}
