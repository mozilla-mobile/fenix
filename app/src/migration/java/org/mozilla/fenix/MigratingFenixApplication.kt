/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.support.migration.FennecMigrator
import mozilla.components.support.migration.MigrationResults

/**
 * An application class which knows how to migrate Fennec data.
 */
class MigratingFenixApplication : FenixApplication() {
    override fun setupInMainProcessOnly() {
        migrateGeckoBlocking()

        setupOperationsNotInvolvingStorageLayers()

        // Some of the operations we need to execute will race with
        // `migrateDataAsynchronously` for access to storage DBs, possibly resulting in SQLITE_BUSY
        // errors.

        // So, storage operations are split away into a separate method,
        // setupOperationsThatAccessStorageLayers.
        // This lets us order things and avoid overlapping storage access.

        CoroutineScope(Dispatchers.Main).launch {
            migrateDataAsync().await()
            setupOperationsThatAccessStorageLayers()
        }
    }

    private fun migrateGeckoBlocking() {
        val migrator = FennecMigrator.Builder(this, this.components.analytics.crashReporter)
            .migrateGecko()
            .build()

        runBlocking {
            migrator.migrateAsync().await()
        }
    }

    private fun migrateDataAsync(): Deferred<MigrationResults> {
        val migrator = FennecMigrator.Builder(this, this.components.analytics.crashReporter)
            .migrateOpenTabs(this.components.core.sessionManager)
            .migrateHistory(this.components.core.historyStorage)
            .migrateBookmarks(this.components.core.bookmarksStorage)
            .migrateFxa(this.components.backgroundServices.accountManager)
            .build()

        return migrator.migrateAsync()
    }
}
