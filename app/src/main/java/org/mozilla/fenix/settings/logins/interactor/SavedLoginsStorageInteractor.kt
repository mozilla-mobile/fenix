/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins.interactor

import org.mozilla.fenix.settings.logins.LoginsFragmentStore
import org.mozilla.fenix.settings.logins.controller.SavedLoginsStorageController

/**
 * Interactor for saved logins storage
 *
 * @property loginsFragmentStore logins state
 * @property viewModel view state for edit login
 * @property loginsController logins storage controller
 */
@SuppressWarnings("TooManyFunctions")
class SavedLoginsStorageInteractor(
    private val loginsFragmentStore: LoginsFragmentStore,
    private val loginsController: SavedLoginsStorageController
) {

}
