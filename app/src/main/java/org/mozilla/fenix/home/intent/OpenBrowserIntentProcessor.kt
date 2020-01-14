/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.intent

import android.content.Intent
import androidx.navigation.NavController
import mozilla.components.support.utils.SafeIntent
import mozilla.components.support.utils.toSafeIntent
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.browser.BrowserNavigation

/**
 * The [org.mozilla.fenix.IntentReceiverActivity] may set the [HomeActivity.OPEN_TO_BROWSER] flag
 * when the browser should be opened in response to an intent.
 */
@Suppress("UNUSED_PARAMETER")
class OpenBrowserIntentProcessor(
    private val getIntentSessionId: (SafeIntent) -> String?
) : HomeIntentProcessor {

    override fun process(intent: Intent, navController: NavController, out: Intent): Boolean {
        return if (intent.extras?.getBoolean(HomeActivity.OPEN_TO_BROWSER) == true) {
            out.putExtra(HomeActivity.OPEN_TO_BROWSER, false)

            BrowserNavigation.openToBrowser(BrowserDirection.FromGlobal, getIntentSessionId(intent.toSafeIntent()))
            true
        } else {
            false
        }
    }
}
