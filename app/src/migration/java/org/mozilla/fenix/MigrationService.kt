/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import mozilla.components.support.migration.AbstractMigrationService

/**
 * Background service for running the migration from legacy Firefox for Android (Fennec).
 */
class MigrationService : AbstractMigrationService() {
    override val migrator by lazy { getMigratorFromApplication() }
}
