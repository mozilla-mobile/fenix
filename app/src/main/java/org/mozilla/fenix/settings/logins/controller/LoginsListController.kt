/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins.controller

import org.mozilla.fenix.settings.logins.LoginsAction
import org.mozilla.fenix.settings.logins.LoginsFragmentStore
import org.mozilla.fenix.settings.logins.SortingStrategy
import org.mozilla.fenix.utils.Settings

/**
 * Controller for the saved logins list
 */
class LoginsListController(
    val loginsFragmentStore: LoginsFragmentStore,
    val settings: Settings
) {
    fun handleSort(sortingStrategy: SortingStrategy) {
        loginsFragmentStore.dispatch(
            LoginsAction.SortLogins(
                sortingStrategy
            )
        )
        settings.savedLoginsSortingStrategy = sortingStrategy
    }
}
