/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.intent

import android.content.Intent
import androidx.navigation.NavController
import org.mozilla.fenix.NavGraphDirections
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.ext.nav

/**
 * Tapping the private browsing mode launcher icon should open Private Mode start screen.
 */
class StartInPrivateModeProcessor(
    private val metrics: MetricController
) : HomeIntentProcessor {

    override fun process(intent: Intent, navController: NavController, out: Intent): Boolean {
        val event = intent.extras?.getBoolean(PRIVATE_BROWSING_PINNED_SHORTCUT)
        return if (event != null) {
            if (event) {
                metrics.track(Event.PrivateBrowsingPinnedShortcutPrivateTab)
            }
            val directions = NavGraphDirections.actionGlobalHomeFragment()
            navController.nav(null, directions)
            true
        } else {
            false
        }
    }

    companion object {
        const val PRIVATE_BROWSING_PINNED_SHORTCUT = "private_browsing_pinned_shortcut"
    }
}
