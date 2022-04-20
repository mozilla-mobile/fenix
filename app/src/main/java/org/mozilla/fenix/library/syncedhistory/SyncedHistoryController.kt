/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.syncedhistory

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.navigateSafe
import org.mozilla.fenix.library.history.History
import org.mozilla.fenix.GleanMetrics.History as GleanHistory
import mozilla.telemetry.glean.private.NoExtras


interface SyncedHistoryController {
    fun handleOpen(item: History)
    fun handleSelect(item: History)
    fun handleDeselect(item: History)
    fun handleBackPressed(): Boolean
    fun handleModeSwitched()
    fun handleSearch()
    fun handleDeleteAll()
    fun handleDeleteSome(items: Set<History>)
}

@Suppress("TooManyFunctions")
class DefaultSyncedHistoryController(
    private val store: SyncedHistoryFragmentStore,
    private val navController: NavController,
    private val openToBrowser: (item: History.Regular) -> Unit,
    private val displayDeleteAll: () -> Unit,
    private val invalidateOptionsMenu: () -> Unit,
    private val deleteHistoryItems: (Set<History>) -> Unit,
) : SyncedHistoryController {

    override fun handleOpen(item: History) {
        when (item) {
            is History.Regular -> openToBrowser(item)
            is History.Group -> {
                GleanHistory.searchTermGroupTapped.record(NoExtras())
                navController.navigate(
                    SyncedHistoryFragmentDirections.actionGlobalHistoryMetadataGroup(
                        title = item.title,
                        historyMetadataItems = item.items.toTypedArray()
                    ),
                    NavOptions.Builder().setPopUpTo(R.id.historyMetadataGroupFragment, true).build()
                )
            }
            else -> { /* noop */ }
        }
    }

    override fun handleSelect(item: History) {
        store.dispatch(SyncedHistoryFragmentAction.AddItemForRemoval(item))
    }

    override fun handleDeselect(item: History) {
        store.dispatch(SyncedHistoryFragmentAction.RemoveItemForRemoval(item))
    }

    override fun handleBackPressed(): Boolean {
        return if (store.state.mode is SyncedHistoryFragmentState.Mode.Editing) {
            store.dispatch(SyncedHistoryFragmentAction.ExitEditMode)
            true
        } else {
            false
        }
    }

    override fun handleModeSwitched() {
        invalidateOptionsMenu.invoke()
    }

    override fun handleSearch() {
        // TODO
        val directions = SyncedHistoryFragmentDirections.actionGlobalHistorySearchDialog()
        navController.navigateSafe(R.id.historyFragment, directions)
    }

    override fun handleDeleteAll() {
        displayDeleteAll.invoke()
    }

    override fun handleDeleteSome(items: Set<History>) {
        deleteHistoryItems.invoke(items)
    }
}