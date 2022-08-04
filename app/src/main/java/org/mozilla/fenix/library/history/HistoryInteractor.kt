/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import org.mozilla.fenix.selection.SelectionInteractor

/**
 * Interface for the HistoryInteractor. This interface is implemented by objects that want
 * to respond to user interaction on the HistoryView
 */
interface HistoryInteractor : SelectionInteractor<History> {

    /**
     * Called on backpressed to exit edit mode
     */
    fun onBackPressed(): Boolean

    /**
     * Called when the mode is switched so we can invalidate the menu
     */
    fun onModeSwitched()

    /**
     * Called when search is tapped
     */
    fun onSearch()

    /**
     * Called when the delete menu button is tapped.
     */
    fun onDeleteTimeRange()

    /**
     * Called when multiple history items are deleted
     *
     * @param items The history items to delete.
     * @param headers The time group headers to hide.
     */
    fun onDeleteHistoryItems(items: Set<History>, headers: Set<HistoryItemTimeGroup> = setOf())

    /**
     * Called when the user has confirmed deletion of a time range.
     *
     * @param timeFrame The selected timeframe. `null` means no specific time frame has been
     * selected; should remove everything.
     */
    fun onDeleteTimeRangeConfirmed(timeFrame: RemoveTimeFrame?)

    /**
     * Called when the user requests a sync of the history
     */
    fun onRequestSync()

    /**
     * Called when the user clicks on recently closed tab button.
     */
    fun onRecentlyClosedClicked()

    /**
     * Called when the user clicks on synced history button.
     */
    fun onSyncedHistoryClicked()

    /**
     * Called when the user clicks on the sign in button.
     */
    fun onSignInClicked()

    /**
     * Called when the user clicks on the create account button.
     */
    fun onCreateAccountClicked()
}

/**
 * Interactor for the history screen
 * Provides implementations for the HistoryInteractor
 */
@SuppressWarnings("TooManyFunctions")
class DefaultHistoryInteractor(
    private val historyController: HistoryController
) : HistoryInteractor {
    override fun open(item: History) {
        historyController.handleOpen(item)
    }

    override fun select(item: History) {
        historyController.handleSelect(item)
    }

    override fun deselect(item: History) {
        historyController.handleDeselect(item)
    }

    override fun onBackPressed(): Boolean {
        return historyController.handleBackPressed()
    }

    override fun onModeSwitched() {
        historyController.handleModeSwitched()
    }

    override fun onSearch() {
        historyController.handleSearch()
    }

    override fun onDeleteTimeRange() {
        historyController.handleDeleteTimeRange()
    }

    override fun onDeleteHistoryItems(items: Set<History>, headers: Set<HistoryItemTimeGroup>) {
        historyController.handleDeleteHistoryItems(items, headers)
    }

    override fun onDeleteTimeRangeConfirmed(timeFrame: RemoveTimeFrame?) {
        historyController.handleDeleteTimeRangeConfirmed(timeFrame)
    }

    override fun onRequestSync() {
        historyController.handleRequestSync()
    }

    override fun onRecentlyClosedClicked() {
        historyController.handleEnterRecentlyClosed()
    }

    override fun onSyncedHistoryClicked() {
        historyController.handleEnterSyncedHistory()
    }

    override fun onSignInClicked() {
        historyController.handleSignIn()
    }

    override fun onCreateAccountClicked() {
        historyController.handleCreateAccount()
    }
}
