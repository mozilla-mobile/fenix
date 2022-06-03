/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.Theme

/**
 * Default layout of a large tab shown in a list taking String arguments for title and caption.
 * Has the following structure:
 * ```
 * ---------------------------------------------
 * | -------------- Title                      |
 * | |    Image   | wrapped on                 |
 * | |    from    | three rows if needed       |
 * | |  imageUrl  |                            |
 * | -------------- Optional caption           |
 * ---------------------------------------------
 * ```
 *
 * @param imageUrl URL from where the to download a header image of the tab this composable renders.
 * @param title Title off the tab this composable renders.
 * @param caption Optional caption text.
 * @param onClick Optional callback to be invoked when this composable is clicked.
 */
@Composable
fun ListItemTabLarge(
    imageUrl: String,
    title: String,
    caption: String? = null,
    onClick: (() -> Unit)? = null
) {
    ListItemTabSurface(imageUrl, onClick) {
        PrimaryText(
            text = title,
            fontSize = 14.sp,
            overflow = TextOverflow.Ellipsis,
            maxLines = 3,
        )

        if (caption != null) {
            SecondaryText(
                text = caption,
                fontSize = 12.sp,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        }
    }
}

/**
 * Default layout of a large tab shown in a list taking composable arguments for title and caption
 * allowing as an exception to customize these elements.
 * Has the following structure:
 * ```
 * ---------------------------------------------
 * | -------------- -------------------------- |
 * | |            | |         Title          | |
 * | |    Image   | |       composable       | |
 * | |    from    | -------------------------- |
 * | |  imageUrl  | -------------------------- |
 * | |            | |   Optional composable  | |
 * | -------------- -------------------------- |
 * ---------------------------------------------
 * ```
 *
 * @param imageUrl URL from where the to download a header image of the tab this composable renders.
 * @param title Composable rendering the title of the tab this composable represents.
 * @param subtitle Optional tab caption composable.
 * @param onClick Optional callback to be invoked when this composable is clicked.
 */
@Composable
fun ListItemTabLarge(
    imageUrl: String,
    onClick: () -> Unit,
    title: @Composable () -> Unit,
    subtitle: @Composable (() -> Unit)? = null
) {
    ListItemTabSurface(imageUrl, onClick) {
        title()

        subtitle?.invoke()
    }
}

/**
 * Shared default configuration of a ListItemTabLarge Composable.
 *
 * @param imageUrl URL from where the to download a header image of the tab this composable renders.
 * @param onClick Optional callback to be invoked when this composable is clicked.
 * @param tabDetails [Composable] Displayed to the the end of the image. Allows for variation in the item text style.
 */
@Composable
fun ListItemTabSurface(
    imageUrl: String,
    onClick: (() -> Unit)? = null,
    tabDetails: @Composable () -> Unit
) {
    var modifier = Modifier.size(328.dp, 116.dp)
    if (onClick != null) modifier = modifier.then(Modifier.clickable { onClick() })

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = FirefoxTheme.colors.layer2,
        elevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            val (imageWidth, imageHeight) = 116.dp to 84.dp
            val imageModifier = Modifier
                .size(imageWidth, imageHeight)
                .clip(RoundedCornerShape(8.dp))

            Image(imageUrl, imageModifier, false, imageWidth)

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                tabDetails()
            }
        }
    }
}

@Composable
@Preview
private fun ListItemTabLargePreview() {
    FirefoxTheme(theme = Theme.getTheme(isPrivate = false)) {
        ListItemTabLarge(
            imageUrl = "",
            title = "This is a very long title for a tab but needs to be so for this preview",
            caption = "And this is a caption"
        ) { }
    }
}

@Composable
@Preview
private fun ListItemTabSurfacePreview() {
    FirefoxTheme(theme = Theme.getTheme(isPrivate = false)) {
        ListItemTabSurface(
            imageUrl = ""
        ) {
            PrimaryText(
                text = "This can be anything",
                fontSize = 22.sp
            )
        }
    }
}
