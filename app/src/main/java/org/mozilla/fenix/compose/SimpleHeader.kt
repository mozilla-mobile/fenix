/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.compose

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * Default header for Cards, Dialogs, Banners, and Homepage.
 * It enforces usage of headline7 typography.
 */
@Composable
fun SimpleHeader(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = FirefoxTheme.colors.textPrimary
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        overflow = TextOverflow.Ellipsis,
        maxLines = 2,
        style = FirefoxTheme.typography.headline7
    )
}

@Composable
@Preview
private fun SimpleHeaderPreview() {
    FirefoxTheme {
        SimpleHeader(text = "Header title")
    }
}
