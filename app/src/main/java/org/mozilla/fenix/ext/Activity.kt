/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowManager
import mozilla.components.support.base.log.Log
import mozilla.components.support.base.log.Log.Priority
import org.mozilla.fenix.perf.Performance

private const val API_LEVEL_ANDROID_9 = 28

/**
 * Attempts to call immersive mode using the View to hide the status bar and navigation buttons.
 *
 * We don't use the equivalent function from Android Components because the stable flag messes
 * with the toolbar. See #1998 and #3272.
 */
fun Activity.enterToImmersiveMode() {
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
}

/**
 * Calls [Activity.reportFullyDrawn] while also preventing crashes under some circumstances.
 */
fun Activity.reportFullyDrawnSafe() {
    // An exception is thrown when calling reportFullyDrawn on some Samsung devices. We could catch
    // the exception but catching exceptions is slow so we disable the call on all Samsung devices
    // except the ones we're using for performance testing.
    //
    // We were unable to identify the root cause of the Exception but suspect it's related to Samsung
    // security features. See
    // https://github.com/mozilla-mobile/fenix/issues/12345#issuecomment-655058864 for details.
    //
    // We briefly measured how slow the Exception code path is here: on a Galaxy S5, it's ~1ms, ~6x
    // slower than the blocklist check. This code might not be worth the time savings but I kept it
    // because we want startup to be as fast as possible.
    val isDeviceInBlocklist = Build.MANUFACTURER == "samsung" &&
        Build.VERSION.SDK_INT == API_LEVEL_ANDROID_9 && // only crashes on Android 9.
        // The S10 can run on Android 9 but doesn't appear to crash and it's a perf reference device
        // so we override for it. Model names can have variations that make them longer so we use contains.
        !Build.MODEL.contains("SM-G97")

    if (isDeviceInBlocklist) {
        // We want this log statement to show up if you grep for fully drawn times so we make it
        // similar to that log statement.
        Log.log(Priority.WARN, Performance.TAG, null, "Fully drawn HARD-CODED DISABLED on this device")
        return
    }

    try {
        reportFullyDrawn()
    } catch (e: SecurityException) {
        // We don't log to Sentry because that will lazily load it and slow down startup.
        Log.log(Priority.ERROR, Performance.TAG, e, "PLEASE FILE A BUG: reportFullyDrawn " +
            "throwing exceptions is unexpected and a minor performance problem.")
    }
}
