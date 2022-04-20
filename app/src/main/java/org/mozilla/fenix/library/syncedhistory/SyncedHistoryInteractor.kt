package org.mozilla.fenix.library.syncedhistory

import org.mozilla.fenix.library.history.History
import org.mozilla.fenix.selection.SelectionInteractor


interface SyncedHistoryInteractor : SelectionInteractor<History> {
    fun onBackPressed(): Boolean
    fun onModeSwitched()
    fun onSearch()
    fun onDeleteAll()
    fun onDeleteSome(items: Set<History>)
}

class DefaultSyncedHistoryInteractor(
    private val historyController: SyncedHistoryController
) : SyncedHistoryInteractor {

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

    override fun onDeleteAll() {
        historyController.handleDeleteAll()
    }

    override fun onDeleteSome(items: Set<History>) {
        historyController.handleDeleteSome(items)
    }
}
