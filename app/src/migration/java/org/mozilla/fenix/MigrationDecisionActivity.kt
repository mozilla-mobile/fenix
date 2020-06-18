/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import android.content.Context
import android.content.Intent
import mozilla.components.support.migration.state.MigrationProgress
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.migration.MigrationProgressActivity

/**
 * The purpose of this activity, when launched, is to decide whether we want to show the migration
 * screen ([MigrationProgressActivity]) or launch the browser normally ([HomeActivity]).
 */
class MigrationDecisionActivity : HomeActivity() {
    private val store by lazy { components.migrationStore }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

        val intent = if (intent != null) intent else Intent()

        // If we don't have the migrating state, we continue with our regular start up flow.
        if (store.state.progress == MigrationProgress.MIGRATING) {
            intent.setClass(applicationContext, MigrationProgressActivity::class.java)
            intent.putExtra(HomeActivity.OPEN_TO_BROWSER, false)

            base.startActivity(intent)
            finish()

            // We are disabling animations here when switching activities because this results in a
            // perceived faster launch. This activity will start immediately with a solid background
            // and then we switch to the actual activity without an animation. This visually looks like
            // a faster start than launching this activity invisibly and switching to the actual
            // activity after that.
            overridePendingTransition(0, 0)
        }
    }
}
