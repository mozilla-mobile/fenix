/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.compose.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.SectionHeader
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * Homepage header.
 *
 * @param text The header string.
 * @param (optional) onShowAllClick Action to take when show all is clicked.
 */
@Composable
fun HomeSectionHeader(
    text: String,
    description: String,
    onShowAllClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SectionHeader(
            text = text,
            modifier = Modifier
                .weight(1f)
                .wrapContentHeight(align = Alignment.Top)
        )

        ClickableText(
            text = AnnotatedString(text = stringResource(id = R.string.recent_tabs_show_all)),
            modifier = Modifier.semantics {
                contentDescription = description
            },
            style = TextStyle(
                color = FirefoxTheme.colors.textAccent,
                fontSize = 14.sp
            ),
            onClick = { onShowAllClick() }
        )
    }
}
