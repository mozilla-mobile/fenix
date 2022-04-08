/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.compose.list

import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mozilla.components.ui.widgets.WidgetSiteItemView
import org.mozilla.fenix.R.drawable
import org.mozilla.fenix.compose.Favicon
import org.mozilla.fenix.compose.PrimaryText
import org.mozilla.fenix.compose.SecondaryText
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.Theme

private val LIST_ITEM_HEIGHT = 56.dp

private val ICON_SIZE = 32.dp

/**
 * List item used to show a website in a list of websites such as history, site exceptions, or
 * collections. This is the equivalent of [WidgetSiteItemView].
 *
 * @param label The label in the list item.
 * @param description An optional description text below the label.
 * @param url Website [url] for which the favicon will be shown.
 * @param onClick Called when the user clicks on the item.
 * @param iconPainter [Painter] used to display a [IconButton] at the end of the list item.
 * @param iconDescription Content description of the icon.
 * @param onIconClick Called when the user clicks on the icon.
 */
@Composable
fun WidgetSiteItem(
    label: String,
    description: String?,
    url: String,
    onClick: (() -> Unit)? = null,
    iconPainter: Painter? = null,
    iconDescription: String? = null,
    onIconClick: (() -> Unit)? = null,
) {
    Row(
        modifier = when (onClick != null) {
            true -> Modifier.clickable { onClick() }
            false -> Modifier
        }.then(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = LIST_ITEM_HEIGHT)
                .padding(start = 16.dp)
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = FirefoxTheme.colors.borderPrimary,
                    shape = RoundedCornerShape(4.dp),
                )
                .padding(8.dp)
        ) {
            Favicon(
                url = url,
                size = 24.dp,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            PrimaryText(
                text = label,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 16.sp,
                maxLines = 1,
            )

            if (description != null) {
                Spacer(modifier = Modifier.height(2.dp))

                SecondaryText(
                    text = description,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
        }

        if (iconPainter != null && onIconClick != null) {
            IconButton(
                onClick = { onIconClick() },
                modifier = Modifier.size(ICON_SIZE)
            ) {
                Icon(
                    painter = iconPainter,
                    contentDescription = iconDescription,
                    tint = FirefoxTheme.colors.iconPrimary,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun WidgetSiteItemDarkPreview() {
    FirefoxTheme(theme = Theme.Dark) {
        Surface(color = FirefoxTheme.colors.layer2) {
            WidgetSiteItem(
                label = "Mozilla-Firefox",
                description = "https://www.mozilla.org/en-US/firefox/whats-new-in-last-version",
                url = "",
                onClick = {},
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun WidgetSiteItemNoCaptionLightPreview() {
    FirefoxTheme(theme = Theme.Light) {
        Surface(
            color = FirefoxTheme.colors.layer2
        ) {
            WidgetSiteItem(
                label = "Mozilla-Firefox",
                description = null,
                url = "",
                onClick = {},
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun WidgetSiteItemWithIconDarkPreview() {
    FirefoxTheme(theme = Theme.Dark) {
        Surface(
            color = FirefoxTheme.colors.layer2
        ) {
            WidgetSiteItem(
                label = "Mozilla-Firefox",
                description = "https://www.mozilla.org/en-US/firefox/whats-new-in-last-version",
                url = "",
                onClick = {},
                iconPainter = painterResource(drawable.ic_close),
                onIconClick = {},
            )
        }
    }
}
