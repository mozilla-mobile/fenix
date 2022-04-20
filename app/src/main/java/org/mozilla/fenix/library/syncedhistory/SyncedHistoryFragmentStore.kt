/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.syncedhistory

import mozilla.components.lib.state.Action
import mozilla.components.lib.state.State
import mozilla.components.lib.state.Store
import org.mozilla.fenix.library.history.History

/**
 * The state for the History Screen
 * @property items List of History to display
 * @property mode Current Mode of History
 * @property pendingDeletionIds History Items to be removed when the Undo snackbar is dismissed
 * @property isDeletingItems Is currently deleting history items from the DataSource
 */
data class SyncedHistoryFragmentState(
    val items: List<History>,
    val mode: Mode,
    val pendingDeletionIds: Set<Long>,
    val isDeletingItems: Boolean
) : State {
    sealed class Mode {
        open val selectedItems = emptySet<History>()
        object Normal : Mode()
        data class Editing(override val selectedItems: Set<History>) : Mode()
    }
}

/**
 * Actions to dispatch through the [HistoryFragmentStore] to modify [SyncedHistoryFragmentState]
 * through the reducer.
 */
sealed class SyncedHistoryFragmentAction : Action {
    object ExitEditMode : SyncedHistoryFragmentAction()
    data class AddItemForRemoval(val item: History) : SyncedHistoryFragmentAction()
    data class RemoveItemForRemoval(val item: History) : SyncedHistoryFragmentAction()
    data class AddPendingDeletionSet(val itemIds: Set<Long>) : SyncedHistoryFragmentAction()
    data class UndoPendingDeletionSet(val itemIds: Set<Long>) : SyncedHistoryFragmentAction()
    object EnterDeleteAllMode : SyncedHistoryFragmentAction()
    object ExitDeleteAllMode : SyncedHistoryFragmentAction()
}

/**
 * The [Store] for holding the [SyncedHistoryFragmentState] and applying [SyncedHistoryFragmentAction].
 */
class SyncedHistoryFragmentStore(initialState: SyncedHistoryFragmentState) :
    Store<SyncedHistoryFragmentState, SyncedHistoryFragmentAction>(initialState, ::syncedHistoryStateReducer)

/**
 * The HistoryState Reducer.
 */
private fun syncedHistoryStateReducer(
    state: SyncedHistoryFragmentState,
    action: SyncedHistoryFragmentAction
): SyncedHistoryFragmentState {
    return when (action) {
        is SyncedHistoryFragmentAction.AddItemForRemoval ->
            state.copy(mode = SyncedHistoryFragmentState.Mode.Editing(state.mode.selectedItems + action.item))
        is SyncedHistoryFragmentAction.RemoveItemForRemoval -> {
            val selected = state.mode.selectedItems - action.item
            state.copy(
                mode = if (selected.isEmpty()) {
                    SyncedHistoryFragmentState.Mode.Normal
                } else {
                    SyncedHistoryFragmentState.Mode.Editing(selected)
                }
            )
        }
        is SyncedHistoryFragmentAction.ExitEditMode -> state.copy(mode = SyncedHistoryFragmentState.Mode.Normal)
        is SyncedHistoryFragmentAction.EnterDeleteAllMode -> state.copy(isDeletingItems = true)
        is SyncedHistoryFragmentAction.ExitDeleteAllMode -> state.copy(isDeletingItems = false)
        is SyncedHistoryFragmentAction.AddPendingDeletionSet ->
            state.copy(pendingDeletionIds = state.pendingDeletionIds + action.itemIds)
        is SyncedHistoryFragmentAction.UndoPendingDeletionSet ->
            state.copy(pendingDeletionIds = state.pendingDeletionIds - action.itemIds)
    }
}