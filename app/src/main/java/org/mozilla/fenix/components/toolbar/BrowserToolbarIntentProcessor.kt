/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import android.content.Intent
import mozilla.components.feature.intent.processing.IntentProcessor
import org.mozilla.fenix.HomeActivity


/**
 * Handle the intent from BrowserToolBar in custom tab
 */
class BrowserToolbarIntentProcessor : IntentProcessor {

    override suspend fun process(intent: Intent): Boolean {
        return intent.extras?.getBoolean(HomeActivity.BROWSER_TOOLBAR_MODE) == true
    }

}