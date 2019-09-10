/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.session

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Bundle
import org.mozilla.fenix.FenixApplication

/**
 * This ActivityLifecycleCallbacks implementations tracks if there is at least one activity in the
 * STARTED state (meaning some part of our application is visible).
 * Based on this information the current task can be removed if the app is not visible.
 */
@SuppressWarnings("EmptyFunctionBlock")
class VisibilityLifecycleCallback(private val activityManager: ActivityManager?) :
    Application.ActivityLifecycleCallbacks {

    /**
     * Activities are not stopped/started in an ordered way. So we are using
     */
    private var activitiesInStartedState: Int = 0

    private fun finishAndRemoveTaskIfInBackground(): Boolean {
        if (activitiesInStartedState == 0) {
            activityManager?.let {
                for (task in it.appTasks) {
                    task.finishAndRemoveTask()
                }
                return true
            }
        }
        return false
    }

    override fun onActivityStarted(activity: Activity?) {
        activitiesInStartedState++
    }

    override fun onActivityStopped(activity: Activity?) {
        activitiesInStartedState--
    }

    override fun onActivityResumed(activity: Activity?) {}

    override fun onActivityPaused(activity: Activity?) {}

    override fun onActivityCreated(activity: Activity?, bundle: Bundle?) {}

    override fun onActivitySaveInstanceState(activity: Activity?, bundle: Bundle?) {}

    override fun onActivityDestroyed(activity: Activity?) {}

    companion object {
        /**
         * If all activities of this app are in the background then finish and remove all tasks. After
         * that the app won't show up in "recent apps" anymore.
         */
        internal fun finishAndRemoveTaskIfInBackground(context: Context): Boolean {
            return (context.applicationContext as FenixApplication)
                .visibilityLifecycleCallback?.finishAndRemoveTaskIfInBackground() ?: false
        }
    }
}
