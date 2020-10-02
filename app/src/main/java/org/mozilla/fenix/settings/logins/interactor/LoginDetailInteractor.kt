/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins.interactor

import org.mozilla.fenix.settings.logins.controller.SavedLoginsStorageController

/**
 * Interactor for the login detail screen
 *
 * @property savedLoginsController controller for the saved logins storage
 */
class LoginDetailInteractor(
    private val savedLoginsController: SavedLoginsStorageController
) {
    fun onFetchLoginList(loginId: String) {
        savedLoginsController.fetchLoginDetails(loginId)
    }

    fun onDeleteLogin(loginId: String) {
        savedLoginsController.delete(loginId)
    }
}
