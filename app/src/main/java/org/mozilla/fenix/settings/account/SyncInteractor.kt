/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.account

/**
 * Interactor for [TurnOnSyncFragment].
 *
 * @param syncController Handles the interactions
 */
class SyncInteractor(private val syncController: SyncController) {
    fun onCameraPermissionsNeeded() {
        syncController.handleCameraPermissionsNeeded()
    }
}
