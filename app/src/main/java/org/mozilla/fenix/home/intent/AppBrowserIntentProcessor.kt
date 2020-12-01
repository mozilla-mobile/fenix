/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.intent

import android.content.Intent
import androidx.navigation.NavController
import mozilla.components.feature.intent.processing.IntentProcessor
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings

/**
 * This handles the case where the browser is opened in response to [Intent.makeMainSelectorActivity]
 * with action [Intent.ACTION_MAIN] and category [Intent.CATEGORY_APP_BROWSER].
 */
class AppBrowserIntentProcessor(
    private val activity: HomeActivity
) : HomeIntentProcessor {

    override fun process(intent: Intent, navController: NavController, out: Intent): Boolean {
        if (intent.selector?.action == Intent.ACTION_MAIN &&
            intent.selector?.categories?.contains(Intent.CATEGORY_APP_BROWSER) == true &&
            !intent.dataString.isNullOrEmpty()
        ) {
            val private = activity.settings().openLinksInAPrivateTab
            if (getIntentProcessor(private).process(intent.toViewIntent())) {
                activity.openToBrowser(BrowserDirection.FromGlobal)
                return true
            }
        }

        return false
    }

    private fun getIntentProcessor(private: Boolean): IntentProcessor {
        return if (private) {
            activity.components.intentProcessors.privateIntentProcessor
        } else {
            activity.components.intentProcessors.intentProcessor
        }
    }

    private fun Intent.toViewIntent() = cloneFilter().apply {
        action = Intent.ACTION_VIEW
        removeCategory(Intent.CATEGORY_LAUNCHER)
    }
}
