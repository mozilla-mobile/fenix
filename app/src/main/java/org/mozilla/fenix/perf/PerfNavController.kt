/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.perf

import androidx.annotation.VisibleForTesting
import androidx.navigation.NavController
import kotlinx.coroutines.Job
import java.util.WeakHashMap

/**
 * We need to keep a weak reference to our NavController in case our activity
 * gets re-created and we need to link our NavGraph to the NavController again.
 * The value of this map is the job associated to the NavController which should link the NavGraph
 * to our NavController.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
val map = WeakHashMap<NavController, Job>()

fun addNavToMap(navController: NavController, job: Job) {
    map[navController] = job
}

/**
 * The job should block the main thread if it isn't completed so that the NavGraph can be loaded
 * before any navigation is done.
 */
fun waitForNavGraphInflation(navController: NavController) {
    map.getValue(navController).also {
        runBlockingIncrement {
            if (!it.isCompleted) {
                it.join()
            }
        }
    }
}
