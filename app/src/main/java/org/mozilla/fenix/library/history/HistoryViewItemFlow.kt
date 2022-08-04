/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import android.content.res.Resources
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.insertFooterItem
import androidx.paging.insertSeparators
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.service.fxa.manager.FxaAccountManager
import org.mozilla.fenix.R
import org.mozilla.fenix.components.history.PagedHistoryProvider

/**
 * Flow of [HistoryViewItem] items used in [HistoryFragment] screen to populate the history list
 * in [HistoryAdapter].
 *
 * @param historyProvider Data source of history and history group items.
 * @param browserStore Provides information about numbers of recently closed tabs.
 * @param isRemote The dataSource can provide local, remote and mixed history.
 * @param resources A class that provides access to String resources for building ViewItems' text
 * fields.
 * @param accountManager Is used to get information if the user is authenticated or not.
 * @param scope Scope used for caching.
 */
class HistoryViewItemFlow(
    historyProvider: PagedHistoryProvider,
    browserStore: BrowserStore,
    isRemote: Boolean? = null,
    resources: Resources,
    accountManager: FxaAccountManager,
    scope: CoroutineScope,
) {
    private val deleteFlow =
        MutableStateFlow(Pair(emptySet<PendingDeletionHistory>(), emptySet<HistoryItemTimeGroup>()))
    // The flow adds an emptyView item as a footer to the list.
    private val emptyStateFlow = MutableStateFlow(false)
    val historyFlow: Flow<PagingData<HistoryViewItem>> = Pager(
        PagingConfig(PAGE_SIZE),
        null
    ) {
        HistoryDataSource(
            historyProvider = historyProvider,
            browserStore = browserStore,
            isRemote = isRemote,
            resources = resources,
            accountManager = accountManager
        )
    }.flow
        .cachedIn(scope)
        // Filtering out items that have been marked for removal.
        .combine(deleteFlow) { historyItems: PagingData<HistoryViewItem>,
            deletedItems: Pair<Set<PendingDeletionHistory>, Set<HistoryItemTimeGroup>> ->
            historyItems.filter { historyItem ->
                when (historyItem) {
                    is HistoryViewItem.HistoryItem -> {
                        deletedItems.first.find { pendingItem ->
                            pendingItem.visitedAt == historyItem.data.visitedAt
                        } == null
                    }
                    is HistoryViewItem.HistoryGroupItem -> {
                        deletedItems.first.find { pendingItem ->
                            pendingItem is PendingDeletionHistory.Group &&
                                pendingItem.visitedAt == historyItem.data.visitedAt
                        } == null
                    }
                    is HistoryViewItem.TimeGroupHeader -> {
                        deletedItems.second.find { historyItemTimeGroup ->
                            historyItem.timeGroup == historyItemTimeGroup
                        } == null
                    }
                    else -> true
                }
            }
        }
        // Adding an empty view. Note that a footer item won't be shown until the end of the list is
        // reached. Because of local/remote item separation, there might be cases when the only
        // visible item is being deleted, but the pager is still trying to load items and footer the
        // item won't be shown until the loading is complete.
        .combine(emptyStateFlow) { historyItems: PagingData<HistoryViewItem>, isEmpty: Boolean ->
            if (isEmpty) {
                historyItems.insertFooterItem(
                    item = HistoryViewItem.EmptyHistoryItem(
                        resources.getString(R.string.history_empty_message)
                    )
                )
            } else {
                historyItems
            }
        }
        // Adding separators for extra space above time group headers.
        .map { pagingData ->
            pagingData.insertSeparators { history: HistoryViewItem?, history2: HistoryViewItem? ->
                if (history2 is HistoryViewItem.TimeGroupHeader) {
                    if (history is HistoryViewItem.TimeGroupHeader ||
                        history is HistoryViewItem.TopSeparatorHistoryItem
                    ) {
                        return@insertSeparators null
                    } else {
                        val separatorTimeGroup = when (history) {
                            is HistoryViewItem.HistoryItem -> history.data.historyTimeGroup
                            is HistoryViewItem.HistoryGroupItem -> history.data.historyTimeGroup
                            else -> null
                        }
                        return@insertSeparators HistoryViewItem.TimeGroupSeparatorHistoryItem(
                            separatorTimeGroup
                        )
                    }
                } else {
                    return@insertSeparators null
                }
            }
        }

    /**
     * Updates [HistoryViewItemFlow.deleteFlow] with the new value, which triggers an update of
     * [HistoryViewItemFlow.historyFlow].
     *
     * @param historyItems Items to be filtered out from the data source.
     * @param historyHeaders Headers to be filtered out from the data source.
     */
    fun setDeleteItems(
        historyItems: Set<PendingDeletionHistory>,
        historyHeaders: Set<HistoryItemTimeGroup>
    ) {
        deleteFlow.value = Pair(historyItems, historyHeaders)
    }

    /**
     * Updates [HistoryViewItemFlow.emptyFlow] with the new value, which triggers an update of
     * [HistoryViewItemFlow.historyFlow].
     *
     * @param isEmpty The new empty state.
     */
    fun setEmptyState(isEmpty: Boolean) {
        emptyStateFlow.value = isEmpty
    }

    companion object {
        const val PAGE_SIZE = 25
    }
}
