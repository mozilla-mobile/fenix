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
import androidx.paging.insertSeparators
import androidx.paging.map
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import mozilla.components.service.fxa.manager.FxaAccountManager
import org.mozilla.fenix.components.history.PagedHistoryProvider

class HistoryViewItemDataSource(
    historyProvider: PagedHistoryProvider,
    historyStore: HistoryFragmentStore,
    isRemote: Boolean? = null,
    context: Context,
    accountManager: FxaAccountManager
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
        .cachedIn(MainScope())
        .combine(collapsedFlow) { a: PagingData<HistoryViewItem>, b: Set<HistoryItemTimeGroup> ->
            a.filter {
                var isVisible = true
                when (it) {
                    is HistoryViewItem.HistoryGroupItem -> it.data.historyTimeGroup
                    is HistoryViewItem.HistoryItem -> it.data.historyTimeGroup
                    else -> null
                }?.let { timeGroup ->
                    isVisible = !b.contains(timeGroup)
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
                            pendingItem is PendingDeletionHistory.Group &&
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
