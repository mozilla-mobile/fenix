/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.syncedhistory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import mozilla.components.support.base.feature.UserInteractionHandler
import org.mozilla.fenix.FeatureFlags
import org.mozilla.fenix.components.StoreProvider
import org.mozilla.fenix.components.history.DefaultPagedHistoryProvider
import org.mozilla.fenix.databinding.FragmentSyncedHistoryBinding
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.library.LibraryPageFragment
import org.mozilla.fenix.library.history.History
import org.mozilla.fenix.library.history.HistoryDataSource
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * A screen displaying history items that were opened on other devices, not local.
 */
class SyncedHistoryFragment : LibraryPageFragment<History>(), UserInteractionHandler {
    private lateinit var syncedHistoryStore: SyncedHistoryFragmentStore
    private lateinit var historyProvider: DefaultPagedHistoryProvider
    private var history: Flow<PagingData<History>> = Pager(
        PagingConfig(PAGE_SIZE),
        null
    ) {
        HistoryDataSource(
            historyProvider = historyProvider,
            isRemote = if (FeatureFlags.showSyncedHistory) true else null
        )
    }.flow

    private var _binding: FragmentSyncedHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSyncedHistoryBinding.inflate(inflater, container, false)

        historyProvider = DefaultPagedHistoryProvider(requireComponents.core.historyStorage)

        syncedHistoryStore = StoreProvider.get(this) {
            SyncedHistoryFragmentStore(
                SyncedHistoryFragmentState(
                    items = listOf(),
                    mode = SyncedHistoryFragmentState.Mode.Normal,
                    pendingDeletionIds = emptySet(),
                    isDeletingItems = false
                )
            )
        }

        val historyController = DefaultSyncedHistoryController(
            store = syncedHistoryStore,
            navController = findNavController(),
            openToBrowser = ::openItem,
            displayDeleteAll = ::displayDeleteAllDialog,
            invalidateOptionsMenu = ::invalidateOptionsMenu,
            deleteHistoryItems = ::deleteHistoryItems,
        )

        val historyInteractor = DefaultSyncedHistoryInteractor(
            historyController
        )

        binding.syncedHistoryLayout.setContent {
            FirefoxTheme {
                HistoryList(
                    history,
                    syncedHistoryStore,
                    historyInteractor
                )
            }
        }

        return binding.root
    }

    private fun openItem(item: History.Regular) {

    }

    private fun displayDeleteAllDialog() {

    }

    private fun invalidateOptionsMenu() {

    }

    private fun deleteHistoryItems(items: Set<History>) {

    }

    override fun onBackPressed(): Boolean {
        return false
    }

    override val selectedItems: Set<History>
        get() = setOf()

    companion object {
        private const val PAGE_SIZE = 25
    }
}
