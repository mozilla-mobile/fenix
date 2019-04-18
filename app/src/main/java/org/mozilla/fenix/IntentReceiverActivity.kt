/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import mozilla.components.browser.session.tab.CustomTabConfig
import mozilla.components.support.utils.SafeIntent
import org.mozilla.fenix.customtabs.CustomTabActivity
import org.mozilla.fenix.ext.components

class IntentReceiverActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The intent property is nullable, but the rest of the code below
        // assumes it is not. If it's null, then we make a new one and open
        // the HomeActivity.
        val intent = intent?.let { Intent(intent) } ?: Intent()

        val isPrivate = PreferenceManager
            .getDefaultSharedPreferences(this).getBoolean(getString(R.string.pref_key_private_mode), false)

        if (isPrivate) {
            components.utils.privateIntentProcessor.process(intent)
        } else {
            components.utils.intentProcessor.process(intent)
        }

        val openToBrowser = when {
            CustomTabConfig.isCustomTabIntent(SafeIntent(intent)) -> {
                intent.setClassName(applicationContext, CustomTabActivity::class.java.name)
                true
            }
            intent.action == Intent.ACTION_VIEW -> {
                intent.setClassName(applicationContext, HomeActivity::class.java.name)
                true
            }
            else -> {
                intent.setClassName(applicationContext, HomeActivity::class.java.name)
                false
            }
        }

        intent.putExtra(HomeActivity.OPEN_TO_BROWSER, openToBrowser)

        startActivity(intent)

        finish()
    }
}
