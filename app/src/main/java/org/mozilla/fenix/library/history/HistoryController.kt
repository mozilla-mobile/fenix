/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import android.content.Context
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.action.HistoryMetadataAction
import mozilla.components.browser.state.action.RecentlyClosedAction
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import mozilla.components.service.glean.private.NoExtras
import org.mozilla.fenix.GleanMetrics.Events
import org.mozilla.fenix.R
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.history.DefaultPagedHistoryProvider
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.settings.account.TurnOnSyncFragment
import org.mozilla.fenix.library.history.HistoryFragment.DeleteConfirmationDialogFragment
import org.mozilla.fenix.utils.Settings
import org.mozilla.fenix.GleanMetrics.History as GleanHistory

@Suppress("TooManyFunctions")
interface HistoryController {
    fun handleOpen(item: History)
    fun handleSelect(item: History)
    fun handleDeselect(item: History)
    fun handleBackPressed(): Boolean
    fun handleModeSwitched()
    fun handleSearch()

    /**
     * Displays a [DeleteConfirmationDialogFragment].
     */
    fun handleDeleteTimeRange()

    /**
     * Hides the deleted items from the UI and triggers the UNDO snackbar.
     * After the snackbar is gone, the items are removed from the data layer.
     *
     * @param items History and History group items to be deleted.
     * @param groups Header to be hidden from the UI.
     */
    fun handleDeleteHistoryItems(items: Set<History>, groups: Set<HistoryItemTimeGroup> = setOf())

    /**
     * Deletes history items inside the time frame.
     *
     * @param timeFrame Selected time frame by the user. If `null`, removes all history.
     */
    fun handleDeleteTimeRangeConfirmed(timeFrame: RemoveTimeFrame?)
    fun handleRequestSync()
    fun handleEnterRecentlyClosed()

    /**
     * Navigates to [HistoryFragment] that would display history synced from other devices.
     */
    fun handleEnterSyncedHistory()

    /**
     * Navigates the user to [TurnOnSyncFragment].
     */
    fun handleSignIn()

    /**
     * Handles a user's request to create an account.
     */
    fun handleCreateAccount()
}

@Suppress("TooManyFunctions", "LongParameterList")
class DefaultHistoryController(
    private val store: HistoryFragmentStore,
    private val appStore: AppStore,
    private val browserStore: BrowserStore,
    private val historyStorage: PlacesHistoryStorage,
    private var historyProvider: DefaultPagedHistoryProvider,
    private val navController: NavController,
    private val scope: CoroutineScope,
    private val openToBrowser: (item: History.Regular) -> Unit,
    private val displayDeleteTimeRange: () -> Unit,
    private val onTimeFrameDeleted: () -> Unit,
    private val invalidateOptionsMenu: () -> Unit,
    private val deleteSnackbar: (
        items: Set<History>,
        undo: suspend (Set<History>) -> Unit,
        delete: (Set<History>) -> suspend (context: Context) -> Unit
    ) -> Unit,
    private val syncHistory: suspend () -> Unit,
    private val createAccount: () -> Unit,
    private val settings: Settings,
) : HistoryController {

    override fun handleOpen(item: History) {
        when (item) {
            is History.Regular -> openToBrowser(item)
            is History.Group -> {
                GleanHistory.searchTermGroupTapped.record(NoExtras())
                navController.navigate(
                    HistoryFragmentDirections.actionGlobalHistoryMetadataGroup(
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
        if (store.state.mode === HistoryFragmentState.Mode.Syncing) {
            return
        }

        store.dispatch(HistoryFragmentAction.AddItemForRemoval(item))
    }

    override fun handleDeselect(item: History) {
        store.dispatch(HistoryFragmentAction.RemoveItemForRemoval(item))
    }

    override fun handleBackPressed(): Boolean {
        return if (store.state.mode is HistoryFragmentState.Mode.Editing) {
            store.dispatch(HistoryFragmentAction.ExitEditMode)
            true
        } else {
            false
        }
    }

    override fun handleModeSwitched() {
        invalidateOptionsMenu.invoke()
    }

    override fun handleSearch() {
        val directions = if (settings.showUnifiedSearchFeature) {
            HistoryFragmentDirections.actionGlobalSearchDialog(null)
        } else {
            HistoryFragmentDirections.actionGlobalHistorySearchDialog()
        }

        navController.navigate(directions)
    }

    override fun handleDeleteTimeRange() {
        displayDeleteTimeRange.invoke()
    }

    override fun handleDeleteHistoryItems(items: Set<History>, groups: Set<HistoryItemTimeGroup>) {
        val pendingDeletionItems = items.map { it.toPendingDeletionHistory() }.toSet()
        appStore.dispatch(AppAction.AddPendingDeletionSet(pendingDeletionItems))
        store.dispatch(HistoryFragmentAction.AddPendingDeletionSet(pendingDeletionItems, groups))
        deleteSnackbar.invoke(items, ::undo, ::delete)
    }

    override fun handleDeleteTimeRangeConfirmed(timeFrame: RemoveTimeFrame?) {
        scope.launch {
            store.dispatch(HistoryFragmentAction.EnterDeletionMode)
            if (timeFrame == null) {
                historyStorage.deleteEverything()
            } else {
                val longRange = timeFrame.toLongRange()
                historyStorage.deleteVisitsBetween(
                    startTime = longRange.first,
                    endTime = longRange.last,
                )
            }
            when (timeFrame) {
                RemoveTimeFrame.LastHour -> GleanHistory.removedLastHour.record(NoExtras())
                RemoveTimeFrame.TodayAndYesterday -> GleanHistory.removedTodayAndYesterday.record(NoExtras())
                null -> GleanHistory.removedAll.record(NoExtras())
            }
            // We introduced more deleting options, but are keeping these actions for all options.
            // The approach could be improved: https://github.com/mozilla-mobile/fenix/issues/26102
            browserStore.dispatch(RecentlyClosedAction.RemoveAllClosedTabAction)
            browserStore.dispatch(EngineAction.PurgeHistoryAction).join()

            store.dispatch(HistoryFragmentAction.ExitDeletionMode)

            launch(Dispatchers.Main) {
                onTimeFrameDeleted.invoke()
            }
        }
    }

    private fun undo(items: Set<History>) {
        val pendingDeletionItems = items.map { it.toPendingDeletionHistory() }.toSet()
        appStore.dispatch(AppAction.UndoPendingDeletionSet(pendingDeletionItems))
        store.dispatch(HistoryFragmentAction.UndoPendingDeletionSet(pendingDeletionItems))
    }

    private fun delete(items: Set<History>): suspend (context: Context) -> Unit {
        return { context ->
            CoroutineScope(Dispatchers.IO).launch {
                store.dispatch(HistoryFragmentAction.EnterDeletionMode)
                for (item in items) {
                    GleanHistory.removed.record(NoExtras())

                    when (item) {
                        is History.Regular -> context.components.core.historyStorage.deleteVisitsFor(item.url)
                        is History.Group -> {
                            // NB: If we have non-search groups, this logic needs to be updated.
                            historyProvider.deleteMetadataSearchGroup(item)
                            context.components.core.store.dispatch(
                                HistoryMetadataAction.DisbandSearchGroupAction(searchTerm = item.title)
                            )
                        }
                        // We won't encounter individual metadata entries outside of groups.
                        is History.Metadata -> {}
                    }
                }
                store.dispatch(HistoryFragmentAction.ExitDeletionMode)
            }
        }
    }

    override fun handleRequestSync() {
        scope.launch {
            store.dispatch(HistoryFragmentAction.StartSync)
            syncHistory.invoke()
            store.dispatch(HistoryFragmentAction.FinishSync)
        }
    }

    override fun handleEnterRecentlyClosed() {
        navController.navigate(
            HistoryFragmentDirections.actionGlobalRecentlyClosed(),
            NavOptions.Builder().setPopUpTo(R.id.recentlyClosedFragment, true).build()
        )
        Events.recentlyClosedTabsOpened.record(NoExtras())
    }

    override fun handleEnterSyncedHistory() {
        navController.navigate(HistoryFragmentDirections.actionSyncedHistoryFragment())
    }

    override fun handleSignIn() {
        navController.navigate(
            HistoryFragmentDirections.actionGlobalTurnOnSync()
        )
    }

    override fun handleCreateAccount() {
        createAccount()
    }
}
