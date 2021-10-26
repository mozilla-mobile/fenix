/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import android.content.DialogInterface
import android.os.Bundle
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.action.RecentlyClosedAction
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.prompt.ShareData
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.service.fxa.sync.SyncReason
import mozilla.components.support.base.feature.UserInteractionHandler
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.NavHostActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.addons.showSnackBar
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.StoreProvider
import org.mozilla.fenix.components.history.DefaultPagedHistoryProvider
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.databinding.FragmentHistoryBinding
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.setTextColor
import org.mozilla.fenix.ext.toShortUrl
import org.mozilla.fenix.library.LibraryPageFragment
import org.mozilla.fenix.utils.allowUndo

@SuppressWarnings("TooManyFunctions", "LargeClass")
class HistoryFragment : LibraryPageFragment<History>(), UserInteractionHandler {
    private lateinit var historyStore: HistoryFragmentStore
    private lateinit var historyInteractor: HistoryInteractor
    private lateinit var viewModel: HistoryViewModel
    private lateinit var historyProvider: DefaultPagedHistoryProvider

    private var undoScope: CoroutineScope? = null
    private var pendingHistoryDeletionJob: (suspend () -> Unit)? = null

    private var _historyView: HistoryView? = null
    protected val historyView: HistoryView
        get() = _historyView!!
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        val view = binding.root
        historyStore = StoreProvider.get(this) {
            HistoryFragmentStore(
                HistoryFragmentState(
                    items = listOf(),
                    mode = HistoryFragmentState.Mode.Normal,
                    pendingDeletionIds = emptySet(),
                    isDeletingItems = false
                )
            )
        }
        val historyController: HistoryController = DefaultHistoryController(
            store = historyStore,
            navController = findNavController(),
            scope = lifecycleScope,
            openToBrowser = ::openItem,
            displayDeleteAll = ::displayDeleteAllDialog,
            invalidateOptionsMenu = ::invalidateOptionsMenu,
            deleteHistoryItems = ::deleteHistoryItems,
            syncHistory = ::syncHistory,
            metrics = requireComponents.analytics.metrics
        )
        historyInteractor = DefaultHistoryInteractor(
            historyController
        )
        _historyView = HistoryView(
            binding.historyLayout,
            historyInteractor
        )

        return view
    }

    override val selectedItems get() = historyStore.state.mode.selectedItems

    private fun invalidateOptionsMenu() {
        activity?.invalidateOptionsMenu()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        historyProvider = DefaultPagedHistoryProvider(requireComponents.core.historyStorage)

        requireComponents.analytics.metrics.track(Event.HistoryOpened)

        setHasOptionsMenu(true)
    }

    private fun deleteHistoryItems(items: Set<History>) {
        updatePendingHistoryToDelete(items)
        undoScope = CoroutineScope(IO)
        undoScope?.allowUndo(
            requireView(),
            getMultiSelectSnackBarMessage(items),
            getString(R.string.bookmark_undo_deletion),
            {
                undoPendingDeletion(items)
            },
            getDeleteHistoryItemsOperation(items)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        consumeFrom(historyStore) {
            historyView.update(it)
        }

        // Data may have been updated in below groups.
        // When returning to this fragment we need to ensure we display the latest data.
        viewModel = HistoryViewModel(historyProvider).also { model ->
            model.userHasHistory.observe(
                viewLifecycleOwner,
                historyView::updateEmptyState
            )

            model.history.observe(
                viewLifecycleOwner,
                historyView.historyAdapter::submitList
            )
        }
    }

    override fun onResume() {
        super.onResume()

        (activity as NavHostActivity).getSupportActionBarAndInflateIfNecessary().show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (historyStore.state.mode is HistoryFragmentState.Mode.Editing) {
            inflater.inflate(R.menu.history_select_multi, menu)
            menu.findItem(R.id.share_history_multi_select)?.isVisible = true
            menu.findItem(R.id.delete_history_multi_select)?.title =
                SpannableString(getString(R.string.bookmark_menu_delete_button)).apply {
                    setTextColor(requireContext(), R.attr.destructive)
                }
        } else {
            inflater.inflate(R.menu.history_menu, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.share_history_multi_select -> {
            val selectedHistory = historyStore.state.mode.selectedItems
            val shareTabs = mutableListOf<ShareData>()

            for (history in selectedHistory) {
                when (history) {
                    is History.Regular -> {
                        shareTabs.add(ShareData(url = history.url, title = history.title))
                    }
                    is History.Group -> {
                        shareTabs.addAll(
                            history.items.map { metadata ->
                                ShareData(url = metadata.url, title = metadata.title)
                            }
                        )
                    }
                    else -> {
                        // no-op, There is no [History.Metadata] in the HistoryFragment.
                    }
                }
            }

            share(shareTabs)

            true
        }
        R.id.delete_history_multi_select -> {
            deleteHistoryItems(historyStore.state.mode.selectedItems)
            historyStore.dispatch(HistoryFragmentAction.ExitEditMode)
            true
        }
        R.id.open_history_in_new_tabs_multi_select -> {
            openItemsInNewTab { selectedItem ->
                selectedItem as History.Regular
                requireComponents.analytics.metrics.track(Event.HistoryOpenedInNewTabs)
                selectedItem.url
            }

            showTabTray()
            true
        }
        R.id.open_history_in_private_tabs_multi_select -> {
            openItemsInNewTab(private = true) { selectedItem ->
                selectedItem as History.Regular
                requireComponents.analytics.metrics.track(Event.HistoryOpenedInPrivateTabs)
                selectedItem.url
            }

            (activity as HomeActivity).apply {
                browsingModeManager.mode = BrowsingMode.Private
                supportActionBar?.hide()
            }

            showTabTray()
            true
        }
        R.id.history_delete_all -> {
            historyInteractor.onDeleteAll()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun showTabTray() {
        invokePendingDeletion()
        findNavController().nav(
            R.id.historyFragment,
            HistoryFragmentDirections.actionGlobalTabsTrayFragment()
        )
    }

    private fun getMultiSelectSnackBarMessage(historyItems: Set<History>): String {
        return if (historyItems.size > 1) {
            getString(R.string.history_delete_multiple_items_snackbar)
        } else {
            val historyItem = historyItems.first()

            String.format(
                requireContext().getString(R.string.history_delete_single_item_snackbar),
                if (historyItem is History.Regular) {
                    historyItem.url.toShortUrl(requireComponents.publicSuffixList)
                } else {
                    historyItem.title
                }
            )
        }
    }

    override fun onPause() {
        invokePendingDeletion()
        super.onPause()
    }

    override fun onBackPressed(): Boolean {
        invokePendingDeletion()
        return historyView.onBackPressed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _historyView = null
        _binding = null
    }

    private fun openItem(item: History.Regular) {
        requireComponents.analytics.metrics.track(Event.HistoryItemOpened)

        (activity as HomeActivity).openToBrowserAndLoad(
            searchTermOrURL = item.url,
            newTab = true,
            from = BrowserDirection.FromHistory
        )
    }

    private fun displayDeleteAllDialog() {
        activity?.let { activity ->
            AlertDialog.Builder(activity).apply {
                setMessage(R.string.delete_browsing_data_prompt_message)
                setNegativeButton(R.string.delete_browsing_data_prompt_cancel) { dialog: DialogInterface, _ ->
                    dialog.cancel()
                }
                setPositiveButton(R.string.delete_browsing_data_prompt_allow) { dialog: DialogInterface, _ ->
                    historyStore.dispatch(HistoryFragmentAction.EnterDeletionMode)
                    // Use fragment's lifecycle; the view may be gone by the time dialog is interacted with.
                    lifecycleScope.launch(IO) {
                        requireComponents.analytics.metrics.track(Event.HistoryAllItemsRemoved)
                        requireComponents.core.store.dispatch(RecentlyClosedAction.RemoveAllClosedTabAction)
                        requireComponents.core.historyStorage.deleteEverything()
                        deleteOpenTabsEngineHistory(requireComponents.core.store)
                        launch(Main) {
                            viewModel.invalidate()
                            historyStore.dispatch(HistoryFragmentAction.ExitDeletionMode)
                            showSnackBar(
                                requireView(),
                                getString(R.string.preferences_delete_browsing_data_snackbar)
                            )
                        }
                    }

                    dialog.dismiss()
                }
                create()
            }.show()
        }
    }

    private suspend fun deleteOpenTabsEngineHistory(store: BrowserStore) {
        store.dispatch(EngineAction.PurgeHistoryAction).join()
    }

    private fun share(data: List<ShareData>) {
        requireComponents.analytics.metrics.track(Event.HistoryItemShared)
        val directions = HistoryFragmentDirections.actionGlobalShareFragment(
            data = data.toTypedArray()
        )
        navigateToHistoryFragment(directions)
    }

    private fun navigateToHistoryFragment(directions: NavDirections) {
        invokePendingDeletion()
        findNavController().nav(
            R.id.historyFragment,
            directions
        )
    }

    private fun getDeleteHistoryItemsOperation(items: Set<History>): (suspend () -> Unit) {
        return {
            CoroutineScope(IO).launch {
                historyStore.dispatch(HistoryFragmentAction.EnterDeletionMode)
                context?.components?.run {
                    for (item in items) {
                        analytics.metrics.track(Event.HistoryItemRemoved)

                        if (item is History.Regular) {
                            core.historyStorage.deleteVisit(
                                url = item.url,
                                timestamp = item.visitedAt
                            )
                        } else if (item is History.Group) {
                            for (historyMetadata in item.items) {
                                historyProvider.getMatchingHistory(historyMetadata)?.let {
                                    core.historyStorage.deleteVisit(
                                        url = it.url,
                                        timestamp = it.visitTime
                                    )
                                }
                            }

                            core.historyStorage.deleteHistoryMetadata(
                                searchTerm = item.title
                            )

                            historyProvider.clearHistoryGroups()
                        }
                    }
                }
                historyStore.dispatch(HistoryFragmentAction.ExitDeletionMode)
                pendingHistoryDeletionJob = null
            }
        }
    }

    private fun updatePendingHistoryToDelete(items: Set<History>) {
        pendingHistoryDeletionJob = getDeleteHistoryItemsOperation(items)
        val ids = items.map { item -> item.visitedAt }.toSet()
        historyStore.dispatch(HistoryFragmentAction.AddPendingDeletionSet(ids))
    }

    private fun undoPendingDeletion(items: Set<History>) {
        pendingHistoryDeletionJob = null
        val ids = items.map { item -> item.visitedAt }.toSet()
        historyStore.dispatch(HistoryFragmentAction.UndoPendingDeletionSet(ids))
    }

    private fun invokePendingDeletion() {
        pendingHistoryDeletionJob?.let {
            viewLifecycleOwner.lifecycleScope.launch {
                it.invoke()
            }.invokeOnCompletion {
                pendingHistoryDeletionJob = null
            }
        }
    }

    private suspend fun syncHistory() {
        val accountManager = requireComponents.backgroundServices.accountManager
        accountManager.syncNow(SyncReason.User)
        viewModel.invalidate()
    }
}
