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
     * Called when bin icon is tapped.
     */
    fun onDeleteTimeRange()

    /**
     * Called when single or multiple history items are set to be deleted.
     * @param items the history items to delete
     * @param headers the time group headers to hide.
     */
    fun onDeleteSome(items: Set<History>, headers: Set<HistoryItemTimeGroup> = setOf())

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
     * Called when the user clicks on a time group header.
     */
    fun onTimeGroupClicked(timeGroup: HistoryItemTimeGroup, collapsed: Boolean)
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

    override fun onDeleteSome(items: Set<History>, headers: Set<HistoryItemTimeGroup>) {
        historyController.handleDeleteSome(items, headers)
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

    override fun onTimeGroupClicked(timeGroup: HistoryItemTimeGroup, collapsed: Boolean) {
        historyController.handleCollapsedStateChanged(timeGroup, collapsed)
    }
}
