/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.perf

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.navigation.NavController
import java.util.WeakHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.fenix.R

/**
 * This class asynchronously loads the Navigation xml.
 *
 * This class is defined as an Object since it needs to be called by the NavController extension
 * function.
 *
 * To use this class properly, NavGraphProvider.inflateNavGraphAsync must be called first before
 * blockForNavGraphInflation using the same navController.
 */
object NavGraphProvider {

    val map = WeakHashMap<NavController, Job>()

    fun inflateNavGraphAsync(navController: NavController, lifecycleScope: LifecycleCoroutineScope) {
        val inflationJob = lifecycleScope.launch(Dispatchers.IO) {
            val inflater = navController.navInflater
            navController.graph = inflater.inflate(R.navigation.nav_graph)
        }

        map[navController] = inflationJob
    }

    /**
     * The job should block the main thread if it isn't completed so that the NavGraph can be loaded
     * before any navigation is done.
     *
     * InflateNavGraphAsync must be called before this method.
     */
    fun blockForNavGraphInflation(navController: NavController) {
        val inflationJob = map[navController] ?: throw IllegalStateException("Expected `NavGraphProvider.inflateNavGraphAsync` " +
                "to be called before this method with the same `navController`. If it was, this may" +
                " be a bug in the implementation.")
        runBlockingIncrement { inflationJob.join() }
    }
}

