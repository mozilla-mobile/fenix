/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.migration

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_migration.*
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.android.content.getColorFromAttr
import mozilla.components.support.migration.AbstractMigrationProgressActivity
import mozilla.components.support.migration.AbstractMigrationService
import mozilla.components.support.migration.MigrationResults
import mozilla.components.support.migration.state.MigrationAction
import mozilla.components.support.migration.state.MigrationProgress
import mozilla.components.support.migration.state.MigrationStore
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.IntentReceiverActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components

class MigrationProgressActivity : AbstractMigrationProgressActivity() {
    private val logger = Logger("MigrationProgressActivity")
    private val statusAdapter = MigrationStatusAdapter()
    override val store: MigrationStore by lazy { components.migrationStore }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_migration)
        init()
    }

    fun init() {
        window.navigationBarColor = getColorFromAttr(R.attr.foundation)

        val appName = migration_description.context.getString(R.string.app_name)

        migration_description.apply {
            text = context.getString(R.string.migration_description, appName)
        }

        migration_status_list.apply {
            val margin = resources.getDimensionPixelSize(R.dimen.migration_margin)
            addItemDecoration(MigrationStatusItemDecoration(margin))
            layoutManager = LinearLayoutManager(this@MigrationProgressActivity)
            adapter = statusAdapter
        }

        migration_welcome_title.apply {
            text = context.getString(R.string.migration_title, appName)
        }

        migration_button_text_view.text = getString(R.string.migration_updating_app_button_text, appName)
    }

    override fun onMigrationCompleted(results: MigrationResults) {
        // Enable clicking the finish button
        migration_button.apply {
            setOnClickListener {
                AbstractMigrationService.dismissNotification(context)

                finish()
                overridePendingTransition(0, 0)

                store.dispatch(MigrationAction.Clear)

                // If we received a user-initiated intent, throw this back to the intent receiver.
                if (intent.hasExtra(HomeActivity.OPEN_TO_BROWSER)) {
                    intent.setClassName(applicationContext, IntentReceiverActivity::class.java.name)
                    startActivity(intent)
                } else {
                    // Fallback: Just launch the browser
                    logger.warn("Intent does not contain OPEN_TO_BROWSER extra, launching HomeActivity")
                    startActivity(Intent(this@MigrationProgressActivity, HomeActivity::class.java))
                }
            }
        }
        migration_button_text_view.apply {
            text = getString(R.string.migration_update_app_button, getString(R.string.app_name))
            setTextColor(ContextCompat.getColor(context, R.color.white_color))
        }
        migration_button.setBackgroundResource(R.drawable.migration_button_background)
        migration_button_progress_bar.visibility = View.INVISIBLE
        // Keep the results list up-to-date.
        statusAdapter.updateData(results)
    }

    override fun onMigrationStateChanged(progress: MigrationProgress, results: MigrationResults) {
        statusAdapter.updateData(results)
    }
}
