/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.intent

import android.content.Intent
import androidx.navigation.NavController
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.ext.components

/**
 * The Private Browsing Mode notification has an "Delete and Open" button to let users delete all
 * of their private tabs.
 */
class NotificationsIntentProcessor(
    private val activity: HomeActivity
) : HomeIntentProcessor {

    override fun process(intent: Intent, navController: NavController, out: Intent): Boolean {
        return if (intent.extras?.getBoolean(HomeActivity.EXTRA_NOTIFICATION) == true) {
            out.putExtra(HomeActivity.EXTRA_NOTIFICATION, false)
            activity.components.core.sessionManager.run {
                this.sessions
                    .filter { it.private }
                    .forEach { this.remove(it) }
            }
            true
        } else {
            false
        }
    }
}
