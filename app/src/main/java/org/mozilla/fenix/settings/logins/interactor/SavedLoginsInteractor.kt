/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins.interactor

import org.mozilla.fenix.settings.logins.SavedLogin
import org.mozilla.fenix.settings.logins.SortingStrategy
import org.mozilla.fenix.settings.logins.controller.LoginsListController

/**
 * Interactor for the saved logins screen
 */
class SavedLoginsInteractor(
    private val loginsListController: LoginsListController,
    private val itemClicked: (SavedLogin) -> Unit,
    private val learnMore: () -> Unit
) {
    fun itemClicked(item: SavedLogin) {
        itemClicked.invoke(item)
    }
    fun onLearnMore() {
        learnMore.invoke()
    }
    fun sort(sortingStrategy: SortingStrategy) {
        loginsListController.handleSort(sortingStrategy)
    }
}
