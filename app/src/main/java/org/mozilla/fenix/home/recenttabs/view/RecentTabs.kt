/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("MagicNumber")

package org.mozilla.fenix.home.recenttabs.view

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mozilla.components.browser.icons.compose.Loader
import mozilla.components.browser.icons.compose.Placeholder
import mozilla.components.browser.icons.compose.WithIcon
import mozilla.components.support.ktx.kotlin.getRepresentativeSnippet
import mozilla.components.ui.colors.PhotonColors
import org.mozilla.fenix.R
import org.mozilla.fenix.components.components
import org.mozilla.fenix.home.recenttabs.RecentTab
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * A list of recent tabs to jump back to.
 *
 * @param recentTabs List of [RecentTab] to display.
 * @param onRecentTabClick Invoked when the user clicks on a recent tab.
 */
@Composable
fun RecentTabs(
    recentTabs: List<RecentTab>,
    onRecentTabClick: (String) -> Unit = {},
    onRecentSearchGroupClicked: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        recentTabs.forEach { tab ->
            when (tab) {
                is RecentTab.Tab -> {
                    RecentTabItem(
                        tabId = tab.tabSessionState.id,
                        url = tab.tabSessionState.content.url,
                        title = tab.tabSessionState.content.title,
                        icon = tab.tabSessionState.content.icon,
                        onRecentTabClick = onRecentTabClick
                    )
                }
                is RecentTab.SearchGroup -> {
                    RecentSearchGroupItem(
                        searchTerm = tab.searchTerm,
                        tabId = tab.tabId,
                        url = tab.url,
                        icon = tab.icon,
                        count = tab.count,
                        onSearchGroupClicked = onRecentSearchGroupClicked
                    )
                }
            }
        }
    }
}

/**
 * A recent tab item.
 *
 * @param tabId The id of the tab.
 * @param url The loaded URL of the tab.
 * @param title The title of the tab.
 * @param icon The icon of the tab.
 * @param onRecentTabClick Invoked when the user clicks on a recent tab.
 */
@Composable
private fun RecentTabItem(
    tabId: String,
    url: String,
    title: String,
    icon: Bitmap? = null,
    onRecentTabClick: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp)
            .clickable { onRecentTabClick(tabId) },
        shape = RoundedCornerShape(8.dp),
        backgroundColor = FirefoxTheme.colors.surface,
        elevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            RecentTabImage(
                url = url,
                modifier = Modifier.size(116.dp, 84.dp),
                icon = icon
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                RecentTabTitle(title = title)

                RecentTabSubtitle(url = url)
            }
        }
    }
}

/**
 * A recent search group item.
 *
 * @param searchTerm The search term for the group.
 * @param tabId The id of the last accessed tab in the group.
 * @param url The loaded URL of the last accessed tab in the group.
 * @param icon The icon of the group.
 * @param count Count of how many tabs belongs to the group.
 * @param onSearchGroupClicked Invoked when the user clicks on a group.
 */
@Suppress("LongParameterList")
@Composable
private fun RecentSearchGroupItem(
    searchTerm: String,
    tabId: String,
    url: String,
    icon: Bitmap?,
    count: Int,
    onSearchGroupClicked: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp)
            .clickable { onSearchGroupClicked(tabId) },
        shape = RoundedCornerShape(8.dp),
        backgroundColor = FirefoxTheme.colors.surface,
        elevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            RecentTabImage(
                url = url,
                modifier = Modifier.size(116.dp, 84.dp),
                icon = icon
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                RecentTabTitle(title = stringResource(R.string.recent_tabs_search_term, searchTerm))

                Row {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_all_tabs),
                        contentDescription = null // decorative element
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    RecentTabSubtitle(url = stringResource(R.string.recent_tabs_search_term_count, count))
                }
            }
        }
    }
}

/**
 * A recent tab image.
 *
 * @param url The loaded URL of the tab.
 * @param modifier modifier Modifier used to draw the image content.
 * @param icon The icon of the tab. Fallback to loading the icon from the [url] if the [icon]
 * is null.
 */
@Composable
private fun RecentTabImage(
    url: String,
    modifier: Modifier = Modifier,
    icon: Bitmap? = null
) {
    if (icon != null) {
        Image(
            painter = BitmapPainter(icon.asImageBitmap()),
            contentDescription = null,
            modifier = modifier,
        )
    } else {
        components.core.icons.Loader(
            url = url
        ) {
            Placeholder {
                Box(
                    modifier = Modifier.background(
                        color = when (isSystemInDarkTheme()) {
                            true -> Color(0xFF42414D) // DarkGrey30
                            false -> PhotonColors.LightGrey30
                        }
                    )
                )
            }

            WithIcon { icon ->
                Image(
                    painter = icon.painter,
                    contentDescription = null,
                    modifier = modifier,
                )
            }
        }
    }
}

/**
 * A recent tab title.
 *
 * @param title The title of the tab.
 */
@Composable
private fun RecentTabTitle(title: String) {
    Text(
        text = title,
        color = FirefoxTheme.colors.textPrimary,
        fontSize = 14.sp,
        overflow = TextOverflow.Ellipsis,
        maxLines = 2
    )
}

/**
 * A recent tab subtitle.
 *
 * @param url The loaded URL of the tab.
 */
@Composable
private fun RecentTabSubtitle(url: String) {
    Text(
        text = url.getRepresentativeSnippet(),
        color = FirefoxTheme.colors.textSecondary,
        fontSize = 12.sp,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1
    )
}
