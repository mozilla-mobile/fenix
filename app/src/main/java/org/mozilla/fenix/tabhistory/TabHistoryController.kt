/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabhistory

import androidx.navigation.NavController
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.support.base.log.logger.Logger

interface TabHistoryController {
    /**
     * Jump to a specific index in the tab's history.
     */
    fun handleGoToHistoryItem(item: TabHistoryItem)

    /**
     * Jump to a specific index in the tab's history in a duplicated tab.
     * [onSuccess] is called on success of the action.
     */
    fun handleGoToHistoryItemNewTab(item: TabHistoryItem, onSuccess: () -> Unit): Boolean
}

class DefaultTabHistoryController(
    private val navController: NavController,
    private val goToHistoryIndexUseCase: SessionUseCases.GoToHistoryIndexUseCase,
    private val duplicateTabUseCase: TabsUseCases.DuplicateTabUseCase,
    private val customTabId: String? = null,
) : TabHistoryController {

    override fun handleGoToHistoryItem(item: TabHistoryItem) {
        navController.navigateUp()

        if (customTabId != null) {
            goToHistoryIndexUseCase.invoke(item.index, customTabId)
        } else {
            goToHistoryIndexUseCase.invoke(item.index)
        }
    }

    override fun handleGoToHistoryItemNewTab(item: TabHistoryItem, onSuccess: () -> Unit): Boolean {
        // Opening a new tab is not supported in custom tabs, so fallback to regular click event
        if (customTabId != null) {
            return false
        }

        val tabId = duplicateTabUseCase.invoke(selectNewTab = true)

        return if (tabId != null) {
            // onSuccess is called first as otherwise haptic feedback would not be performed
            onSuccess.invoke()
            handleGoToHistoryItem(item)
            true
        } else {
            Logger.error("$this: could not access selected tab to duplicate.")
            false
        }
    }
}
