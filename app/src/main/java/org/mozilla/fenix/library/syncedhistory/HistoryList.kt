/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.syncedhistory

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.Favicon
import org.mozilla.fenix.compose.PrimaryText
import org.mozilla.fenix.compose.SecondaryText
import org.mozilla.fenix.library.history.History
import org.mozilla.fenix.library.history.HistoryItemTimeGroup
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.Theme

/**
 * Top-level list UI for displaying History items.
 *
 * @param history Source of items to be displayed.
 */

@Suppress("LongMethod", "ComplexMethod")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryList(
    history: Flow<PagingData<History>>
) {
    val historyItems: LazyPagingItems<History> = history.collectAsLazyPagingItems()

    val headerPositions: MutableMap<HistoryItemTimeGroup, Int> = mutableMapOf()
    val collapsedState = remember {
        mutableStateMapOf(
            HistoryItemTimeGroup.Today to false,
            HistoryItemTimeGroup.Yesterday to false,
            HistoryItemTimeGroup.ThisWeek to false,
            HistoryItemTimeGroup.ThisMonth to false,
            HistoryItemTimeGroup.Older to false
        )
    }
    val context = LocalContext.current

    LazyColumn(
        contentPadding = PaddingValues(top = 8.dp)
    ) {
        val itemCount = historyItems.itemCount
        if (itemCount == 0) {
            item {
                NoHistoryItem(scope = this)
            }
            return@LazyColumn
        }

        // Calculating headers' positions and titles. Peek method allows to check the data without
        // triggering the page load and any drawing.
        val headerTitles: MutableMap<Int, String> = mutableMapOf()
        for (index in 0 until itemCount) {
            val historyItem = historyItems.peek(index) ?: continue

            var possibleTimeGroup: HistoryItemTimeGroup? = null
            if (headerPositions.containsKey(historyItem.historyTimeGroup)) {
                if (index <= headerPositions[historyItem.historyTimeGroup] as Int) {
                    headerPositions[historyItem.historyTimeGroup] = index
                    possibleTimeGroup = historyItem.historyTimeGroup
                }
            } else {
                headerPositions[historyItem.historyTimeGroup] = index
                possibleTimeGroup = historyItem.historyTimeGroup
            }

            possibleTimeGroup?.let {
                headerTitles[index] = it.humanReadable(context)
            }
        }

        for (index in 0 until itemCount) {
            // Triggers the page load if needed
            val historyItem = historyItems[index] ?: continue

            // If the index is present, draw a sticky header.
            if (headerTitles.containsKey(index)) {
                this@LazyColumn.stickyHeader(key = index) {
                    HistorySectionHeader(
                        headerTitles[index]!!,
                        expanded = collapsedState[historyItem.historyTimeGroup]
                    ) {
                        val currentValue = collapsedState[historyItem.historyTimeGroup]!!
                        collapsedState[historyItem.historyTimeGroup] = !currentValue
                    }
                }
            }

            item {
                // Skip drawing any items below a collapsed header.
                if (collapsedState[historyItem.historyTimeGroup] == true) return@item

                when (historyItem) {
                    is History.Regular -> {
                        HistoryItem(historyItem.title, historyItem.url)
                    }
                    is History.Metadata -> {
                        HistoryItem(historyItem.title, historyItem.url)
                    }
                    is History.Group -> {
                        val numChildren = historyItem.items.size
                        val stringId = if (numChildren == 1) {
                            R.string.history_search_group_site
                        } else {
                            R.string.history_search_group_sites
                        }
                        val tabCount = String.format(LocalContext.current.getString(stringId), numChildren)
                        HistoryGroupItem(historyItem.title, tabCount)
                    }
                }

                // Giving a header extra space above it when drawn inside the list. It allows to
                // keep it's regular height in sticky mode.
                if (headerTitles.containsKey(index + 1) && index != 0) {
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

/**
 * Collapsible header for sections of history items.
 *
 * @param headerText The section title for a group of history tabs.
 * @param expanded Indicates whether the section of content is expanded. If null, the Icon will be hidden.
 * @param onClick Optional lambda for handling section header clicks.
 */
@Composable
fun HistorySectionHeader(
    headerText: String,
    expanded: Boolean?,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .height(48.dp)
            .background(FirefoxTheme.colors.layer1)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .height(47.dp)
                .fillMaxWidth()
        ) {
            PrimaryText(
                text = headerText,
                modifier = Modifier.padding(top = 14.dp, start = 16.dp),
                fontSize = 16.sp,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )

            Spacer(Modifier.weight(1f))

            expanded?.let {
                Icon(
                    modifier = Modifier.padding(top = 14.dp, end = 16.dp),
                    painter = painterResource(
                        if (expanded) R.drawable.ic_chevron_down else R.drawable.ic_chevron_up
                    ),
                    contentDescription = stringResource(
                        if (expanded) R.string.history_synced_collapse_group else R.string.history_synced_expand_group
                    ),
                    tint = FirefoxTheme.colors.iconPrimary,
                )
            }
        }

        Divider(color = FirefoxTheme.colors.borderPrimary)
    }
}

/**
 * History list item UI.
 *
 * @param titleText The history item title.
 * @param url The history item URL.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryItem(
    titleText: String,
    url: String
) {
    Row(
        modifier = Modifier.height(56.dp)
    ) {
        Spacer(Modifier.width(16.dp))

        Box(
            modifier = Modifier.padding(top = 17.dp)
        ) {
            Favicon(
                url = url,
                size = 24.dp
            )
        }

        Spacer(modifier = Modifier.width(32.dp))

        Column(
            Modifier.weight(1f)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            PrimaryText(
                text = titleText,
                fontSize = 16.sp,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            SecondaryText(
                text = url,
                fontSize = 14.sp,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }

        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.history_delete_item),
                tint = FirefoxTheme.colors.iconPrimary,
            )
        }
    }
}

/**
 * History group list item UI.
 *
 * @param titleText The history group item title.
 * @param tabCount The history group number of tabs.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryGroupItem(
    titleText: String,
    tabCount: String
) {
    Row(
        modifier = Modifier.height(56.dp)
    ) {
        Spacer(Modifier.width(16.dp))

        Image(
            modifier = Modifier.padding(top = 17.dp),
            painter = painterResource(R.drawable.ic_multiple_tabs),
            contentDescription = null
        )

        Spacer(Modifier.width(32.dp))

        Column(
            Modifier.weight(1f)
        ) {
            Spacer(Modifier.height(8.dp))

            PrimaryText(
                text = titleText,
                fontSize = 16.sp,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )

            Spacer(Modifier.height(2.dp))

            SecondaryText(
                text = tabCount,
                fontSize = 14.sp,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }

        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.history_delete_item),
                tint = FirefoxTheme.colors.iconPrimary,
            )
        }
    }
}

/**
 * UI to be displayed when a user's device has no history.
 */
@Composable
fun NoHistoryItem(scope: LazyItemScope) {
    with(scope) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillParentMaxHeight()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            SecondaryText(
                text = stringResource(R.string.history_empty_message),
                fontSize = 16.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun SyncedTabsListPreview() {
    val context = LocalContext.current
    FirefoxTheme(theme = Theme.getTheme(isPrivate = false)) {
        Column(Modifier.background(FirefoxTheme.colors.layer1)) {
            HistorySectionHeader(
                headerText = HistoryItemTimeGroup.Today.humanReadable(context),
                expanded = true
            )
            HistoryItem(
                titleText = "www.google.com",
                url = "https://www.google.com"
            )
            HistoryGroupItem(
                titleText = "cats",
                tabCount = "2 sites"
            )
        }
    }
}
