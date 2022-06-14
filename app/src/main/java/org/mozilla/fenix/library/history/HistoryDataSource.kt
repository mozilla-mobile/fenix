/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.mozilla.fenix.R
import org.mozilla.fenix.components.history.HistoryDB
import org.mozilla.fenix.components.history.PagedHistoryProvider
import org.mozilla.fenix.ext.components
import java.util.*
import kotlin.RuntimeException
import kotlin.collections.HashMap

/**
 * PagingSource of History items, used in History Screen. It is the data source for the
 * Flow<PagingData>, that provides HistoryAdapter with items to display.
 */
class HistoryDataSource(
    private val historyProvider: PagedHistoryProvider,
    private var historyStore: HistoryFragmentStore,
    private val isRemote: Boolean? = null,
    private val context: Context
) : PagingSource<Int, HistoryViewItem>() {

    private lateinit var headerPositionsNew: SortedMap<HistoryItemTimeGroup, Int>

    // The refresh key is set to null so that it will always reload the entire list for any data
    // updates such as pull to refresh, and return the user to the start of the list.
    override fun getRefreshKey(state: PagingState<Int, HistoryViewItem>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, HistoryViewItem> {
        // Get the offset of the last loaded page or default to 0 when it is null on the initial
        // load or a refresh.
        val offset = params.key ?: 0
        var previousHistory: History? = null
        if (offset == 0) {
            headerPositionsNew = TreeMap()
        }

        var isEmpty = false
        val headerPositions: MutableList<Pair<HistoryViewItem.TimeGroupHeader, Int>> = LinkedList()

        Log.d("kalabak", "HistoryDataSource, params.loadSize = ${params.loadSize}")
        val historyItems = historyProvider.getHistory(offset, params.loadSize, isRemote).run {
            Log.d("kolobok", "historyItems size = ${this.size}, loadSize = ${params.loadSize}")
            if (size == 0 && params.key == null) {
                isEmpty = true
            }
            positionWithOffset(offset)
        }.mapIndexed { position, history ->

            previousHistory?.let {
                if (it.historyTimeGroup != history.historyTimeGroup) {
                    if (!headerPositionsNew.contains(history.historyTimeGroup)) {
                        val header = HistoryViewItem.TimeGroupHeader(
                            title = history.historyTimeGroup.humanReadable(context),
                            timeGroup = history.historyTimeGroup,
                            collapsed = historyStore.state.collapsedHeaders.contains(history.historyTimeGroup)//collapsedHeaders.contains(secondTimeGroup)
                        )
                        headerPositionsNew[history.historyTimeGroup] = position
                        headerPositions.add(Pair(header, position))
                    }
                }
            } ?: run {
                if (!headerPositionsNew.contains(history.historyTimeGroup)) {
                    val header = HistoryViewItem.TimeGroupHeader(
                        title = history.historyTimeGroup.humanReadable(context),
                        timeGroup = history.historyTimeGroup,
                        collapsed = historyStore.state.collapsedHeaders.contains(history.historyTimeGroup)//collapsedHeaders.contains(secondTimeGroup)
                    )
                    headerPositionsNew[history.historyTimeGroup] = position
                    headerPositions.add(Pair(header, position))
                }
            }

//            if (position == 0) {
//                val header = HistoryViewItem.TimeGroupHeader(
//                    title = history.historyTimeGroup.humanReadable(context),
//                    timeGroup = history.historyTimeGroup,
//                    collapsed = historyStore.state.collapsedHeaders.contains(history.historyTimeGroup)//collapsedHeaders.contains(secondTimeGroup)
//                )
//                headerPositions.add(Pair(header, position))
//            }

            when (history) {
                is History.Regular -> HistoryViewItem.HistoryItem(history)
                is History.Group -> HistoryViewItem.HistoryGroupItem(history)
                is History.Metadata -> throw RuntimeException("Not supported!")
            }.apply {
                previousHistory = history
            }
        }.let {
            val mutableList = it.toMutableList()

            for (header in headerPositions.reversed()) {
                mutableList.add(header.second, header.first)
            }

            if (params.key == null) {
                if (isRemote == false) {
                    mutableList.add(
                        0,
                        HistoryViewItem.SyncedHistoryItem(
                            context.getString(R.string.history_synced_from_other_devices)
                        )
                    )
                }

                if (isRemote == false || isRemote == null) {
                    val numRecentTabs = context.components.core.store.state.closedTabs.size
                    mutableList.add(
                        0,
                        HistoryViewItem.RecentlyClosedItem(
                            context.getString(R.string.library_recently_closed_tabs),
                            String.format(
                                context.getString(
                                    if (numRecentTabs == 1) {
                                        R.string.recently_closed_tab
                                    } else {
                                        R.string.recently_closed_tabs
                                    }
                                ),
                                numRecentTabs
                            )
                        )
                    )
                }
            }

//            mutableList

//            if (params.key == null && isRemote == false) {
//
//                mutableList.add(
//                    0,
//                    HistoryViewItem.RecentlyClosedItem(
//                        context.getString(R.string.history_synced_from_other_devices),
//                        String.format(
//                            context.getString(
//                                if (numRecentTabs == 1) {
//                                    R.string.recently_closed_tab
//                                } else {
//                                    R.string.recently_closed_tabs
//                                }
//                            ),
//                            numRecentTabs
//                        )
//                    )
//                )
//
//                mutableList

//                val numRecentTabs = context.components.core.store.state.closedTabs.size
//                it.toMutableList().apply {
//                    add(
//                        0,
//                        HistoryViewItem.RecentlyClosedItem(
//                            context.getString(R.string.history_synced_from_other_devices),
//                            String.format(
//                                context.getString(
//                                    if (numRecentTabs == 1) {
//                                        R.string.recently_closed_tab
//                                    } else {
//                                        R.string.recently_closed_tabs
//                                    }
//                                ),
//                                numRecentTabs
//                            )
//                        )
//                    )
//                    add(
//                        0,
//                        HistoryViewItem.SyncedHistoryItem(
//                            context.getString(R.string.history_synced_from_other_devices)
//                        )
//                    )
//                }

//                val mutableList = it.toMutableList()
//
//                val numRecentTabs = context.components.core.store.state.closedTabs.size
//                mutableList.add(
//                    HistoryViewItem.RecentlyClosedItem(
//                        context.getString(R.string.history_synced_from_other_devices),
//                        String.format(
//                            context.getString(
//                                if (numRecentTabs == 1) {
//                                    R.string.recently_closed_tab
//                                } else {
//                                    R.string.recently_closed_tabs
//                                }
//                            ),
//                            numRecentTabs
//                        )
//                    )
//                )
//
//                mutableList.add(
//                    HistoryViewItem.SyncedHistoryItem(
//                        context.getString(R.string.history_synced_from_other_devices)
//                    )
//                )
//                mutableList
//            } else {
//                mutableList
//            }
                mutableList
        }.let {
            if (isEmpty) {
                it.add(
                    HistoryViewItem.EmptyHistoryItem(
                        context.getString(R.string.history_empty_message)
                    )
                )
            }
            it
        }

//        if (offset == 0 && historyItems.size > 1) {
//            val firstItem = historyItems[0]
//            val firstItemTimeGroup = if (firstItem is HistoryViewItem.HistoryItem) {
//                firstItem.data.historyTimeGroup
//            } else if (firstItem is HistoryViewItem.HistoryGroupItem) {
//                firstItem.data.historyTimeGroup
//            } else {
//                throw RuntimeException()
//            }
//
//            val header = HistoryViewItem.TimeGroupHeader(
//                title = firstItemTimeGroup.humanReadable(context),
//                timeGroup = firstItemTimeGroup,
//                collapsed = historyStore.state.collapsedHeaders.contains(firstItemTimeGroup)//collapsedHeaders.contains(secondTimeGroup)
//            )
//            val temp = historyItems.toMutableList()
//            temp.add(0, header)
//            historyItems = temp
//        }

        if (params.key == null && historyItems.isEmpty()) {

        }

        val nextOffset = if (historyItems.isEmpty()) {
            null
        } else {
            offset + params.loadSize
        }
        Log.d("kolobok", "nextOffset = $nextOffset")

        return LoadResult.Page(
            data = historyItems,
            prevKey = null, // Only paging forward.
            nextKey = nextOffset
        )
    }
}

@VisibleForTesting
internal fun List<HistoryDB>.positionWithOffset(offset: Int): List<History> {
    return this.foldIndexed(listOf()) { index, prev, item ->
        // Only offset once while folding, so that we don't accumulate the offset for each element.
        val itemOffset = if (index == 0) {
            offset
        } else {
            0
        }
        val previousPosition = prev.lastOrNull()?.position ?: 0
        when (item) {
            is HistoryDB.Group -> {
                // XXX considering an empty group to have a non-zero offset is the obvious
                // limitation of the current approach, and indicates that we're conflating
                // two concepts here - position of an element for the sake of a RecyclerView,
                // and an offset for the sake of our history pagination API.
                val groupOffset = if (item.items.isEmpty()) {
                    1
                } else {
                    item.items.size
                }
                prev + item.positioned(position = previousPosition + itemOffset + groupOffset)
            }
            is HistoryDB.Metadata -> {
                prev + item.positioned(previousPosition + itemOffset + 1)
            }
            is HistoryDB.Regular -> {
                prev + item.positioned(previousPosition + itemOffset + 1)
            }
        }
    }
}

private fun HistoryDB.Group.positioned(position: Int): History.Group {
    return History.Group(
        position = position,
        items = this.items.mapIndexed { index, item -> item.positioned(index) },
        title = this.title,
        visitedAt = this.visitedAt,
        historyTimeGroup = this.historyTimeGroup,
    )
}

private fun HistoryDB.Metadata.positioned(position: Int): History.Metadata {
    return History.Metadata(
        position = position,
        historyMetadataKey = this.historyMetadataKey,
        title = this.title,
        totalViewTime = this.totalViewTime,
        url = this.url,
        visitedAt = this.visitedAt,
        historyTimeGroup = this.historyTimeGroup,
    )
}

private fun HistoryDB.Regular.positioned(position: Int): History.Regular {
    return History.Regular(
        position = position,
        title = this.title,
        url = this.url,
        visitedAt = this.visitedAt,
        historyTimeGroup = this.historyTimeGroup,
    )
}
