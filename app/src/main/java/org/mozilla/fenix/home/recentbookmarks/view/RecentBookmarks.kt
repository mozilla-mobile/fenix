/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.recentbookmarks.view

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mozilla.components.browser.icons.compose.Loader
import mozilla.components.browser.icons.compose.Placeholder
import mozilla.components.browser.icons.compose.WithIcon
import mozilla.components.ui.colors.PhotonColors
import org.mozilla.fenix.components.components
import org.mozilla.fenix.compose.Image
import org.mozilla.fenix.compose.inComposePreview
import org.mozilla.fenix.home.recentbookmarks.RecentBookmark
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.Theme

private val cardShape = RoundedCornerShape(8.dp)

private val imageWidth = 126.dp

private val imageModifier = Modifier
    .size(width = imageWidth, height = 82.dp)
    .clip(cardShape)

/**
 * A list of recent bookmarks.
 *
 * @param bookmarks List of [RecentBookmark]s to display.
 * @param menuItems List of [RecentBookmarksMenuItem] shown when long clicking a [RecentBookmarkItem]
 * @param backgroundColor The background [Color] of each bookmark.
 * @param onRecentBookmarkClick Invoked when the user clicks on a recent bookmark.
 * @param onRecentBookmarkLongClick Invoked when the user long clicks on a recent bookmark.
 */
@Composable
fun RecentBookmarks(
    bookmarks: List<RecentBookmark>,
    menuItems: List<RecentBookmarksMenuItem>,
    backgroundColor: Color,
    onRecentBookmarkClick: (RecentBookmark) -> Unit = {},
    onRecentBookmarkLongClick: () -> Unit = {},
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(bookmarks) { bookmark ->
            RecentBookmarkItem(
                bookmark = bookmark,
                menuItems = menuItems,
                backgroundColor = backgroundColor,
                onRecentBookmarkClick = onRecentBookmarkClick,
                onRecentBookmarkLongClick = onRecentBookmarkLongClick,
            )
        }
    }
}

/**
 * A recent bookmark item.
 *
 * @param bookmark The [RecentBookmark] to display.
 * @param menuItems The list of [RecentBookmarksMenuItem] shown when long clicking on the recent bookmark item.
 * @param backgroundColor The background [Color] of the recent bookmark item.
 * @param onRecentBookmarkClick Invoked when the user clicks on the recent bookmark item.
 * @param onRecentBookmarkLongClick Invoked when the user long clicks on the recent bookmark item.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentBookmarkItem(
    bookmark: RecentBookmark,
    menuItems: List<RecentBookmarksMenuItem>,
    backgroundColor: Color,
    onRecentBookmarkClick: (RecentBookmark) -> Unit = {},
    onRecentBookmarkLongClick: () -> Unit = {},
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .width(158.dp)
            .combinedClickable(
                enabled = true,
                onClick = { onRecentBookmarkClick(bookmark) },
                onLongClick = {
                    onRecentBookmarkLongClick()
                    isMenuExpanded = true
                },
            ),
        shape = cardShape,
        backgroundColor = backgroundColor,
        elevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        ) {
            RecentBookmarkImage(bookmark)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = bookmark.title ?: bookmark.url ?: "",
                color = FirefoxTheme.colors.textPrimary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = FirefoxTheme.typography.caption,
            )

            RecentBookmarksMenu(
                showMenu = isMenuExpanded,
                menuItems = menuItems,
                recentBookmark = bookmark,
                onDismissRequest = { isMenuExpanded = false },
            )
        }
    }
}

@Composable
private fun RecentBookmarkImage(bookmark: RecentBookmark) {
    when {
        !bookmark.previewImageUrl.isNullOrEmpty() -> {
            Image(
                url = bookmark.previewImageUrl,
                modifier = imageModifier,
                targetSize = imageWidth,
                contentScale = ContentScale.Crop,
            )
        }
        !bookmark.url.isNullOrEmpty() && !inComposePreview -> {
            components.core.icons.Loader(bookmark.url) {
                Placeholder {
                    PlaceholderBookmarkImage()
                }

                WithIcon { icon ->
                    Box(
                        modifier = imageModifier.background(
                            color = when (isSystemInDarkTheme()) {
                                true -> PhotonColors.DarkGrey60
                                false -> PhotonColors.LightGrey30
                            },
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = icon.painter,
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(cardShape),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
        }
        inComposePreview -> {
            PlaceholderBookmarkImage()
        }
    }
}

@Composable
private fun PlaceholderBookmarkImage() {
    Box(
        modifier = imageModifier.background(
            color = when (isSystemInDarkTheme()) {
                true -> PhotonColors.DarkGrey60
                false -> PhotonColors.LightGrey30
            },
        ),
    )
}

/**
 * Menu shown for a [RecentBookmark].
 *
 * @see [DropdownMenu]
 *
 * @param showMenu Whether this is currently open and visible to the user.
 * @param menuItems List of options shown.
 * @param recentBookmark The [RecentBookmark] for which this menu is shown.
 * @param onDismissRequest Called when the user chooses a menu option or requests to dismiss the menu.
 */
@Composable
private fun RecentBookmarksMenu(
    showMenu: Boolean,
    menuItems: List<RecentBookmarksMenuItem>,
    recentBookmark: RecentBookmark,
    onDismissRequest: () -> Unit,
) {
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { onDismissRequest() },
        modifier = Modifier
            .background(color = FirefoxTheme.colors.layer2),
    ) {
        for (item in menuItems) {
            DropdownMenuItem(
                onClick = {
                    onDismissRequest()
                    item.onClick(recentBookmark)
                },
            ) {
                Text(
                    text = item.title,
                    color = FirefoxTheme.colors.textPrimary,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterVertically),
                )
            }
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun RecentBookmarksPreview() {
    FirefoxTheme(theme = Theme.getTheme()) {
        RecentBookmarks(
            bookmarks = listOf(
                RecentBookmark(
                    title = "Other Bookmark Title",
                    url = "https://www.example.com",
                    previewImageUrl = null,
                ),
                RecentBookmark(
                    title = "Other Bookmark Title",
                    url = "https://www.example.com",
                    previewImageUrl = null,
                ),
                RecentBookmark(
                    title = "Other Bookmark Title",
                    url = "https://www.example.com",
                    previewImageUrl = null,
                ),
                RecentBookmark(
                    title = "Other Bookmark Title",
                    url = "https://www.example.com",
                    previewImageUrl = null,
                ),
            ),
            menuItems = listOf(),
            backgroundColor = FirefoxTheme.colors.layer2,
        )
    }
}
