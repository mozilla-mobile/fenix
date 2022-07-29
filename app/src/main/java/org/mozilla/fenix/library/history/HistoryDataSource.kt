/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import android.content.res.Resources
import androidx.paging.PagingSource
import androidx.paging.PagingState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.service.fxa.manager.FxaAccountManager
import org.mozilla.fenix.R
import org.mozilla.fenix.components.history.HistoryDB
import org.mozilla.fenix.components.history.PagedHistoryProvider
import java.util.SortedMap
import java.util.TreeMap
import java.util.LinkedList

/**
 * PagingSource of [HistoryViewItem], used in [HistoryFragment] to display history.
 * It is the backbone of [HistoryViewItemFlow] data stream, that provides [HistoryAdapter] with
 * items to display.
 *
 * @param historyProvider Data source of history and history group items.
 * @param browserStore Provides information about numbers of recently closed tabs.
 * @param isRemote The dataSource can provide local, remote and mixed history.
 * @param resources A class that provides access to String resources for building ViewItems' text
 * fields.
 * @param accountManager Is used to get information if the user is authenticated or not.
 */
class HistoryDataSource(
    private val historyProvider: PagedHistoryProvider,
    private val browserStore: BrowserStore,
    private val isRemote: Boolean? = null,
    private val resources: Resources,
    private val accountManager: FxaAccountManager,
) : PagingSource<Int, HistoryViewItem>() {

    // A map that helps to insure that headers don't get duplicated. It is cleared upon pull to
    // refresh.
    private lateinit var headerPositions: SortedMap<HistoryItemTimeGroup, Int>

    // The refresh key is set to null so that it will always reload the entire list for any data
    // updates such as pull to refresh, and return the user to the start of the list.
    override fun getRefreshKey(state: PagingState<Int, HistoryViewItem>): Int? = null

    @Suppress("LongMethod", "ComplexMethod")
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, HistoryViewItem> {
        // Get the offset of the last loaded page or default to 0 when it is null on the initial
        // load or a refresh.
        val offset = params.key ?: 0
        if (offset == 0) {
            headerPositions = TreeMap()
        }

        // If the user isn't authenticated, we display the SignIn view and stop loading.
        if (isRemote == true && accountManager.authenticatedAccount() == null) {
            return LoadResult.Page(
                data = listOf(HistoryViewItem.SignInHistoryItem),
                prevKey = null,
                nextKey = null
            )
        }

        var finishedLoading = false
        var previousItem: History? = null
        val headerPositions: MutableList<Pair<HistoryViewItem.TimeGroupHeader, Int>> = LinkedList()

        val historyItems = historyProvider.getHistory(offset, params.loadSize, null).run {
            if (size == 0) {
                finishedLoading = true
            }
            // We want to get all of the items of local/remote type. A single payload might contain
            // items only of the wrong type, which stops the pagination. Hence, filtering is done here.
            filter {
                if (isRemote == true) {
                    // Filtering only remote history items.
                    when (it) {
                        is HistoryDB.Regular -> it.isRemote
                        is HistoryDB.Group -> false // Groups are always local.
                        else -> true
                    }
                } else if (isRemote == false) {
                    // Filtering local history items and history groups.
                    if (it is HistoryDB.Regular) {
                        !it.isRemote
                    } else {
                        true
                    }
                } else {
                    // Otherwise, passing down both.
                    true
                }
            }.positionWithOffset(offset)
        }.mapIndexed { position, history ->
            // Calculating header positions.
            previousItem?.let {
                // Calculating header positions between items.
                val isHeaderRequired = it.historyTimeGroup != history.historyTimeGroup &&
                    !this.headerPositions.contains(history.historyTimeGroup)
                if (isHeaderRequired) {
                    val header = HistoryViewItem.TimeGroupHeader(
                        title = history.historyTimeGroup.humanReadable(resources),
                        timeGroup = history.historyTimeGroup,
                    )
                    this.headerPositions[history.historyTimeGroup] = position
                    headerPositions.add(Pair(header, position))
                }
            } ?: run {
                // Checking if a header before the first item is needed.
                if (!this.headerPositions.contains(history.historyTimeGroup)) {
                    val header = HistoryViewItem.TimeGroupHeader(
                        title = history.historyTimeGroup.humanReadable(resources),
                        timeGroup = history.historyTimeGroup,
                    )
                    this.headerPositions[history.historyTimeGroup] = position
                    headerPositions.add(Pair(header, position))
                }
            }

            when (history) {
                is History.Regular -> HistoryViewItem.HistoryItem(history)
                is History.Group -> HistoryViewItem.HistoryGroupItem(history)
                is History.Metadata -> throw IllegalStateException("Unexpected dataType.")
            }.apply {
                previousItem = history
            }
        }.let {
            // Adding headers.
            val mutableList = it.toMutableList()
            for (header in headerPositions.reversed()) {
                mutableList.add(header.second, header.first)
            }
            // Adding synced and recently closed buttons.
            val isFirstLoad = params.key == null
            if (isFirstLoad) {
                if (isRemote == false) {
                    mutableList.add(
                        0,
                        HistoryViewItem.SyncedHistoryItem(
                            resources.getString(R.string.history_synced_from_other_devices)
                        )
                    )
                }
                // For local or mixed history show RecentlyClosed button.
                if (isRemote == false || isRemote == null) {
                    val numRecentTabs = browserStore.state.closedTabs.size
                    mutableList.add(
                        0,
                        HistoryViewItem.RecentlyClosedItem(
                            resources.getString(R.string.library_recently_closed_tabs),
                            String.format(
                                resources.getString(
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
                // Adding a top spacing.
                mutableList.add(0, HistoryViewItem.TopSeparatorHistoryItem)
            }
            mutableList
        }

        val nextOffset = if (finishedLoading) {
            null
        } else {
            offset + params.loadSize
        }

        return LoadResult.Page(
            data = historyItems,
            prevKey = null, // Only paging forward.
            nextKey = nextOffset
        )
    }
}

private fun List<HistoryDB>.positionWithOffset(offset: Int): List<History> {
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

internal fun HistoryDB.Regular.positioned(position: Int): History.Regular {
    return History.Regular(
        position = position,
        title = this.title,
        url = this.url,
        visitedAt = this.visitedAt,
        historyTimeGroup = this.historyTimeGroup,
    )
}
