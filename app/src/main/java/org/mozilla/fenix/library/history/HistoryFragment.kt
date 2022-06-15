/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.SpannableString
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.insertSeparators
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.action.RecentlyClosedAction
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.prompt.ShareData
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.service.fxa.sync.SyncReason
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.telemetry.glean.private.NoExtras
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.FeatureFlags
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.NavHostActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.addons.showSnackBar
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.StoreProvider
import org.mozilla.fenix.components.history.DefaultPagedHistoryProvider
import org.mozilla.fenix.databinding.FragmentHistoryBinding
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.setTextColor
import org.mozilla.fenix.ext.toShortUrl
import org.mozilla.fenix.ext.getRootView
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.library.LibraryPageFragment
import org.mozilla.fenix.utils.allowUndo
import java.lang.RuntimeException
import org.mozilla.fenix.GleanMetrics.History as GleanHistory

@SuppressWarnings("TooManyFunctions", "LargeClass")
class HistoryFragment : LibraryPageFragment<History>(), UserInteractionHandler {
    private lateinit var historyStore: HistoryFragmentStore
    private lateinit var historyInteractor: HistoryInteractor
    private lateinit var historyProvider: DefaultPagedHistoryProvider
    private lateinit var historyDataSource: HistoryDataSource
    private var collapsedHeaders: Set<HistoryItemTimeGroup> = setOf()
    private val isSyncedHistory: Boolean by lazy { arguments?.getBoolean("isSyncedHistory") ?: false }

    private val collapsedFlow = MutableStateFlow(collapsedHeaders)
    private val deleteFlow = MutableStateFlow(Pair(emptySet<PendingDeletionHistory>(), emptySet<HistoryItemTimeGroup>()))
    private val emptyFlow = MutableStateFlow(false)

    private var history: Flow<PagingData<HistoryViewItem>> = Pager(
        PagingConfig(PAGE_SIZE),
        null
    ) {
        historyDataSource = HistoryDataSource(
            historyProvider = historyProvider,
            historyStore = historyStore,
            isRemote = if (FeatureFlags.showSyncedHistory) isSyncedHistory else null,
            context = requireContext()
        )
        historyDataSource
    }.flow
        .cachedIn(MainScope())
//        .map { pagingData ->
//            pagingData.map { history ->
////                Log.d("kolobok", "historyTimeGroup = " + history.historyTimeGroup.humanReadable(context = requireContext()))
//                Log.d(
//                    "kolobok",
//                    "historyTimeGroup = " + history.historyTimeGroup.humanReadable(context = requireContext()) + "history = $history"
//                )
//
//                when (history) {
//                    is History.Regular -> HistoryViewItem.HistoryItem(history)
//                    is History.Group -> HistoryViewItem.HistoryGroupItem(history)
//                    is History.Metadata -> throw RuntimeException("Not supported!")
//                }
//            }
//        }
//        .map { pagingData ->
//            pagingData.insertSeparators { history: HistoryViewItem?, history2: HistoryViewItem? ->
////                if (history == null && history2 != null) {
////                    val secondTimeGroup = when (history2) {
////                        is HistoryViewItem.HistoryItem -> history2.data.historyTimeGroup
////                        is HistoryViewItem.HistoryGroupItem -> history2.data.historyTimeGroup
////                        else -> throw RuntimeException()
////                    }
////                    return@insertSeparators HistoryViewItem.TimeGroupHeader(
////                        title = secondTimeGroup.humanReadable(requireContext()),
////                        timeGroup = secondTimeGroup,
////                        collapsed = historyStore.state.collapsedHeaders.contains(secondTimeGroup)//collapsedHeaders.contains(secondTimeGroup)
////                    )
////                }
//
//                if (history == null || history2 == null) {
//                    return@insertSeparators null
//                }
//                val firstTimeGroup = when (history) {
//                    is HistoryViewItem.HistoryItem -> history.data.historyTimeGroup
//                    is HistoryViewItem.HistoryGroupItem -> history.data.historyTimeGroup
//                    else -> throw RuntimeException()
//                }
//
//                val secondTimeGroup = when (history2) {
//                    is HistoryViewItem.HistoryItem -> history2.data.historyTimeGroup
//                    is HistoryViewItem.HistoryGroupItem -> history2.data.historyTimeGroup
//                    else -> throw RuntimeException()
//                }
//
//                if (firstTimeGroup != secondTimeGroup) {
//                    return@insertSeparators HistoryViewItem.TimeGroupHeader(
//                        title = secondTimeGroup.humanReadable(requireContext()),
//                        timeGroup = secondTimeGroup,
//                        collapsed = historyStore.state.collapsedHeaders.contains(secondTimeGroup) //collapsedHeaders.contains(secondTimeGroup)
//                    )
//                }
//
//                return@insertSeparators null
//            }
//        }
        .combine(collapsedFlow) { a: PagingData<HistoryViewItem>, b: Set<HistoryItemTimeGroup> ->
            a.filter {
                var isVisible = true
                when (it) {
                    is HistoryViewItem.HistoryGroupItem -> it.data.historyTimeGroup
                    is HistoryViewItem.HistoryItem -> it.data.historyTimeGroup
                    else -> null
                }?.let { timeGroup ->
                    isVisible = !b.contains(timeGroup)
//                    Log.d("CollapseDebugging", "filter, contains = $isVisible")
                }
                isVisible
            }
        }
        .combine(deleteFlow) { a: PagingData<HistoryViewItem>, b: Pair<Set<PendingDeletionHistory>, Set<HistoryItemTimeGroup>> ->
            a.filter {
                when (it) {
                    is HistoryViewItem.HistoryItem -> {
                        b.first.find { pendingItem ->
                            pendingItem.visitedAt == it.data.visitedAt
                        } == null
                    }
                    is HistoryViewItem.HistoryGroupItem -> {
                        b.first.find { pendingItem ->
                            pendingItem.visitedAt == it.data.visitedAt
                        } == null
                    }
                    is HistoryViewItem.TimeGroupHeader -> {
                        b.second.find { historyItemTimeGroup ->
                            it.timeGroup == historyItemTimeGroup
                        } == null
                    }
                    else -> true
                }
            }
        }.combine(emptyFlow) { a: PagingData<HistoryViewItem>, b: Boolean ->
            a.filter {
                if (it is HistoryViewItem.EmptyHistoryItem) {
                    b
                } else {
                    true
                }
            }
        }
//        .map { pagingData ->
//            pagingData.insertSeparators { history: HistoryViewItem?, history2: HistoryViewItem? ->
////                if (history == null && history2 != null) {
////                    val secondTimeGroup = when (history2) {
////                        is HistoryViewItem.HistoryItem -> history2.data.historyTimeGroup
////                        is HistoryViewItem.HistoryGroupItem -> history2.data.historyTimeGroup
////                        else -> throw RuntimeException()
////                    }
////                    return@insertSeparators HistoryViewItem.TimeGroupHeader(
////                        title = secondTimeGroup.humanReadable(requireContext()),
////                        timeGroup = secondTimeGroup,
////                        collapsed = historyStore.state.collapsedHeaders.contains(secondTimeGroup)//collapsedHeaders.contains(secondTimeGroup)
////                    )
////                }
//
//                if (history == null || history2 == null) {
//                    return@insertSeparators null
//                }
//                val firstTimeGroup = when (history) {
//                    is HistoryViewItem.HistoryItem -> history.data.historyTimeGroup
//                    is HistoryViewItem.HistoryGroupItem -> history.data.historyTimeGroup
//                    else -> throw RuntimeException()
//                }
//
//                val secondTimeGroup = when (history2) {
//                    is HistoryViewItem.HistoryItem -> history2.data.historyTimeGroup
//                    is HistoryViewItem.HistoryGroupItem -> history2.data.historyTimeGroup
//                    else -> throw RuntimeException()
//                }
//
//                if (firstTimeGroup != secondTimeGroup) {
//                    return@insertSeparators HistoryViewItem.TimeGroupHeader(
//                        title = secondTimeGroup.humanReadable(requireContext()),
//                        timeGroup = secondTimeGroup,
//                        collapsed = historyStore.state.collapsedHeaders.contains(secondTimeGroup) //collapsedHeaders.contains(secondTimeGroup)
//                    )
//                }
//
//                return@insertSeparators null
//            }
//        }


//        .map { pagingData ->
//            if (!isSyncedHistory) {
//                pagingData.insertSeparators { history: HistoryViewItem?, history2: HistoryViewItem? ->
//                    if (history == null && history2 != null) {
//                        return@insertSeparators HistoryViewItem.SyncedHistoryItem(
//                            getString(R.string.history_synced_from_other_devices)
//                        )
//                    }
//                    return@insertSeparators null
//                }
//            } else {
//                pagingData
//            }
//        }.map { pagingData ->
//            if (!isSyncedHistory) {
//                pagingData.insertSeparators { history: HistoryViewItem?, history2: HistoryViewItem? ->
//                    if (history == null && history2 != null) {
//                        val numRecentTabs = requireContext().components.core.store.state.closedTabs.size
//                        return@insertSeparators HistoryViewItem.RecentlyClosedItem(
//                            getString(R.string.history_synced_from_other_devices),
//                            String.format(
//                                requireContext().getString(
//                                    if (numRecentTabs == 1) {
//                                        R.string.recently_closed_tab
//                                    } else {
//                                        R.string.recently_closed_tabs
//                                    }
//                                ),
//                                numRecentTabs
//                            )
//                        )
//                    }
//                    return@insertSeparators null
//                }
//            } else {
//                pagingData
//            }
//        }

    private var _historyView: HistoryView? = null
    private val historyView: HistoryView
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
                    pendingDeletionItems = emptySet(),
                    isEmpty = false,
                    isDeletingItems = false,
                    collapsedHeaders
                )
            )
        }
        val historyController: HistoryController = DefaultHistoryController(
            store = historyStore,
            appStore = requireContext().components.appStore,
            historyProvider = historyProvider,
            navController = findNavController(),
            scope = lifecycleScope,
            openToBrowser = ::openItem,
            displayDeleteAll = ::displayDeleteAllDialog,
            invalidateOptionsMenu = ::invalidateOptionsMenu,
            deleteSnackbar = ::deleteSnackbar,
            syncHistory = ::syncHistory,
        )
        historyInteractor = DefaultHistoryInteractor(
            historyController
        )
        _historyView = HistoryView(
            binding.historyLayout,
            historyInteractor,
            onZeroItemsLoaded = {
                historyStore.dispatch(
                    HistoryFragmentAction.ChangeEmptyState(isEmpty = true)
                )
            },
            onEmptyStateChanged = {
                historyStore.dispatch(
                    HistoryFragmentAction.ChangeEmptyState(it)
                )
            },
            invalidateHistoryDataSource = {
                historyDataSource.invalidate()
            },
            isSyncedHistory = isSyncedHistory
        )

        return view
    }

    /**
     * All the current selected items. Individual history entries and entries from a group.
     * When a history group is selected, this will instead contain all the history entries in that group.
     */
    override val selectedItems
        get() = historyStore.state.mode.selectedItems.fold(emptyList<History>()) { accumulator, item ->
            when (item) {
                is History.Group -> accumulator + item.items
                else -> accumulator + item
            }
        }.toSet()

    private fun invalidateOptionsMenu() {
        activity?.invalidateOptionsMenu()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        historyProvider = DefaultPagedHistoryProvider(requireComponents.core.historyStorage)

        GleanHistory.opened.record(NoExtras())

        setHasOptionsMenu(true)
    }

    private fun deleteSnackbar(
        items: Set<History>,
        undo: suspend (items: Set<History>) -> Unit,
        delete: (Set<History>) -> suspend (context: Context) -> Unit
    ) {
        CoroutineScope(IO).allowUndo(
            requireActivity().getRootView()!!,
            getMultiSelectSnackBarMessage(items),
            getString(R.string.snackbar_deleted_undo),
            {
                undo.invoke(items)
            },
            delete(items)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        consumeFrom(historyStore) {
            historyView.update(it)
            if (collapsedHeaders != it.collapsedHeaders) {
//                collapsedFlow.compareAndSet(collapsedHeaders, collapsedHeaders)
                collapsedFlow.value = it.collapsedHeaders
            }

            collapsedHeaders = it.collapsedHeaders

            deleteFlow.value = Pair(it.pendingDeletionItems, it.hiddenHeaders)
            Log.d("MOSCOW", "isEmpty = ${it.isEmpty}")
            emptyFlow.value = it.isEmpty
        }

        requireContext().components.appStore.flowScoped(viewLifecycleOwner) { flow ->
            flow.mapNotNull { state -> state.pendingDeletionHistoryItems }.collect { items ->
//                historyStore.dispatch(
//                    HistoryFragmentAction.UpdatePendingDeletionItems(pendingDeletionItems = items)
//                )
//                deleteFlow.value = items // TODO old delete
            }
        }

        lifecycleScope.launch {
            history.collectLatest {
                historyView.historyAdapter.submitData(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
//        (activity as NavHostActivity).getSupportActionBarAndInflateIfNecessary().show()

        val title = if (isSyncedHistory) {
            getString(R.string.history_from_other_devices)
        } else {
            getString(R.string.library_history)
        }
        showToolbar(title)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (historyStore.state.mode is HistoryFragmentState.Mode.Editing) {
            inflater.inflate(R.menu.history_select_multi, menu)
            menu.findItem(R.id.share_history_multi_select)?.isVisible = true
            menu.findItem(R.id.delete_history_multi_select)?.title =
                SpannableString(getString(R.string.bookmark_menu_delete_button)).apply {
                    setTextColor(requireContext(), R.attr.textWarning)
                }
        } else {
            inflater.inflate(R.menu.history_menu, menu)
        }

        if (!FeatureFlags.historyImprovementFeatures) {
            menu.findItem(R.id.history_search)?.isVisible = false
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
            historyInteractor.onDeleteSome(historyStore.state.mode.selectedItems)
            historyStore.dispatch(HistoryFragmentAction.ExitEditMode)
            true
        }
        R.id.open_history_in_new_tabs_multi_select -> {
            openItemsInNewTab { selectedItem ->
                GleanHistory.openedItemsInNewTabs.record(NoExtras())
                (selectedItem as? History.Regular)?.url ?: (selectedItem as? History.Metadata)?.url
            }

            showTabTray()
            true
        }
        R.id.open_history_in_private_tabs_multi_select -> {
            openItemsInNewTab(private = true) { selectedItem ->
                GleanHistory.openedItemsInNewTabs.record(NoExtras())
                (selectedItem as? History.Regular)?.url ?: (selectedItem as? History.Metadata)?.url
            }

            (activity as HomeActivity).apply {
                browsingModeManager.mode = BrowsingMode.Private
                supportActionBar?.hide()
            }

            showTabTray()
            true
        }
        R.id.history_search -> {
            GleanHistory.searchIconTapped.record(NoExtras())
            historyInteractor.onSearch()
            true
        }
        R.id.history_delete_all -> {
            historyInteractor.onDeleteAll()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun showTabTray() {
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

    override fun onBackPressed(): Boolean {
        return historyView.onBackPressed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _historyView = null
        _binding = null
    }

    private fun openItem(item: History.Regular) {
        GleanHistory.openedItem.record(NoExtras())

        (activity as HomeActivity).openToBrowserAndLoad(
            searchTermOrURL = item.url,
            newTab = true,
            from = BrowserDirection.FromHistory
        )
    }

    private fun displayDeleteAllDialog() {
        activity?.let { activity ->
            AlertDialog.Builder(activity).apply {
                val layout = LayoutInflater.from(context).inflate(R.layout.delete_history_time_range_dialog, null)
                val radioGroup = layout.findViewById<RadioGroup>(R.id.radio_group)
                radioGroup.check(R.id.last_hour_button)
                setView(layout)

                setNegativeButton(R.string.delete_browsing_data_prompt_cancel) { dialog: DialogInterface, _ ->
                    dialog.cancel()
                }
                setPositiveButton(R.string.delete_browsing_data_prompt_allow) { dialog: DialogInterface, _ ->
                    if (radioGroup.checkedRadioButtonId == R.id.everything_button) {
                        historyStore.dispatch(HistoryFragmentAction.EnterDeletionMode)
                        // Use fragment's lifecycle; the view may be gone by the time dialog is interacted with.
                        lifecycleScope.launch(IO) {
                            GleanHistory.removedAll.record(NoExtras())
                            requireComponents.core.store.dispatch(RecentlyClosedAction.RemoveAllClosedTabAction)
                            requireComponents.core.historyStorage.deleteEverything()
                            deleteOpenTabsEngineHistory(requireComponents.core.store)
                            launch(Main) {
                                historyView.historyAdapter.refresh()
                                historyStore.dispatch(HistoryFragmentAction.ExitDeletionMode)
                                showSnackBar(
                                    requireView(),
                                    getString(R.string.preferences_delete_browsing_data_snackbar)
                                )
                            }
                        }
                    } else {
                        val timeFrame = when (radioGroup.checkedRadioButtonId) {
                            R.id.last_hour_button -> RemoveTimeGroup.OneHour.timeFrameForTimeGroup()
                            R.id.today_and_yesterday_button -> RemoveTimeGroup.TodayAndYesterday.timeFrameForTimeGroup()
                            else -> throw RuntimeException("Unexpected view id")
                        }
                        historyStore.dispatch(HistoryFragmentAction.EnterDeletionMode)
                        // Use fragment's lifecycle; the view may be gone by the time dialog is interacted with.
                        lifecycleScope.launch(IO) {
                            requireComponents.core.historyStorage.deleteVisitsBetween(
                                startTime = timeFrame.first,
                                endTime = timeFrame.second
                            )
                            launch(Main) {
                                historyView.historyAdapter.refresh()
                                historyStore.dispatch(HistoryFragmentAction.ExitDeletionMode)
                                showSnackBar(
                                    requireView(),
                                    getString(R.string.preferences_delete_browsing_data_snackbar)
                                )
                            }
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
        GleanHistory.shared.record(NoExtras())
        val directions = HistoryFragmentDirections.actionGlobalShareFragment(
            data = data.toTypedArray()
        )
        navigateToHistoryFragment(directions)
    }

    private fun navigateToHistoryFragment(directions: NavDirections) {
        findNavController().nav(
            R.id.historyFragment,
            directions
        )
    }

    @Suppress("UnusedPrivateMember")
    private suspend fun syncHistory() {
        val accountManager = requireComponents.backgroundServices.accountManager
        accountManager.syncNow(SyncReason.User)
        historyView.historyAdapter.refresh()
    }

    @Suppress("UnusedPrivateMember")
    companion object {
        private const val PAGE_SIZE = 25
    }
}
