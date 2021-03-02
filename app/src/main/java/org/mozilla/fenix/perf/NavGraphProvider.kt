/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.perf

import androidx.annotation.VisibleForTesting
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.mozilla.fenix.R
import java.util.WeakHashMap

/**
 * This class asynchronously loads the Navigation xml. It also keeps track of the NavController
 * to which the inflated NavGraph belongs too.
 */
object NavGraphProvider {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var map = WeakHashMap<NavController, Job>()

    /**
     * Asynchronously inflate the NavGraph on the IO dispatcher and insert the new job in the map
     * with the navcontroller as its key.
     */
    fun inflateNavGraphAsync(navHostFragment: NavHostFragment, job: Job? = null) {
        val inflationJob = job ?: CoroutineScope(Dispatchers.IO).launch {
            val inflater = navHostFragment.navController.navInflater
            navHostFragment.navController.graph = inflater.inflate(R.navigation.nav_graph)
        }

        map[navHostFragment.navController] = inflationJob
    }

    /**
     * The job should block the main thread if it isn't completed so that the NavGraph can be loaded
     * before any navigation is done.
     */
    fun blockForNavGraphInflation(navController: NavController) {
        val inflationJob = map[navController] ?: throw NoSuchElementException("The NavController " +
                "is missing in the map. This might be caused the async job not being started ")
        runBlockingIncrement { inflationJob.join() }
    }


    fun onActivityDestroyRemoveJobs(){
        map.values.forEach { it.cancel() }
    }
}

