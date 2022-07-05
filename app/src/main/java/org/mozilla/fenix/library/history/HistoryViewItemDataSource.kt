/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.insertFooterItem
import androidx.paging.insertSeparators
import androidx.paging.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import mozilla.components.service.fxa.manager.FxaAccountManager
import org.mozilla.fenix.R
import org.mozilla.fenix.components.history.PagedHistoryProvider

/**
 * Flow of [HistoryViewItem] used in History Screen to populate the [HistoryAdapter].
 */
class HistoryViewItemDataSource(
    historyProvider: PagedHistoryProvider,
    historyStore: HistoryFragmentStore,
    isRemote: Boolean? = null,
    context: Context,
    accountManager: FxaAccountManager,
    scope: CoroutineScope
) {
    private var collapsedHeaders: Set<HistoryItemTimeGroup> = setOf()
    private val collapsedFlow = MutableStateFlow(collapsedHeaders)
    private val deleteFlow =
        MutableStateFlow(Pair(emptySet<PendingDeletionHistory>(), emptySet<HistoryItemTimeGroup>()))
    private val emptyFlow = MutableStateFlow(false)

    val historyFlow: Flow<PagingData<HistoryViewItem>> = Pager(
        PagingConfig(PAGE_SIZE),
        null
    ) {
        HistoryDataSource(
            historyProvider = historyProvider,
            historyStore = historyStore,
            isRemote = isRemote,
            context = context,
            accountManager = accountManager
        )
    }.flow
        .cachedIn(scope)
        // Filtering out history items that have a collapsed timeGroup, and changing collapsed state
        // for headers.
        .combine(collapsedFlow) { historyItems: PagingData<HistoryViewItem>, timeGroups: Set<HistoryItemTimeGroup> ->
            historyItems.filter { historyItem ->
                var isVisible = true
                val timeGroup = when (historyItem) {
                    is HistoryViewItem.HistoryGroupItem -> historyItem.data.historyTimeGroup
                    is HistoryViewItem.HistoryItem -> historyItem.data.historyTimeGroup
                    else -> null
                }
                if (timeGroup != null) {
                    isVisible = !timeGroups.contains(timeGroup)
                }
                isVisible
            }.map {
                if (it is HistoryViewItem.TimeGroupHeader) {
                    it.copy(collapsed = collapsedHeaders.contains(it.timeGroup))
                } else {
                    it
                }
            }
        }
        // Filtering out items that have been marked for removal.
        .combine(deleteFlow) { historyItems: PagingData<HistoryViewItem>, deletedItems: Pair<Set<PendingDeletionHistory>, Set<HistoryItemTimeGroup>> ->
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
        .combine(emptyFlow) { historyItems: PagingData<HistoryViewItem>, isEmpty: Boolean ->
            if (isEmpty) {
                historyItems.insertFooterItem(
                    item = HistoryViewItem.EmptyHistoryItem(
                        context.getString(R.string.history_empty_message)
                    )
                )
            } else {
                historyItems
            }
        }
        // Adding separators for extra space above not collapsed time group headers.
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

    fun setCollapsedHeaders(collapsedHeaders: Set<HistoryItemTimeGroup>) {
        this.collapsedHeaders = collapsedHeaders
        collapsedFlow.value = collapsedHeaders
    }

    fun setDeleteItems(
        historyItems: Set<PendingDeletionHistory>,
        historyHeaders: Set<HistoryItemTimeGroup>
    ) {
        deleteFlow.value = Pair(historyItems, historyHeaders)
    }

    fun setEmptyState(isEmpty: Boolean) {
        emptyFlow.value = isEmpty
    }

    companion object {
        private const val PAGE_SIZE = 25
    }
}
