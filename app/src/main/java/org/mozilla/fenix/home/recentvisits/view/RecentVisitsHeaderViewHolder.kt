/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.recentvisits.view

import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.ComposeViewHolder
import org.mozilla.fenix.compose.SectionHeader
import org.mozilla.fenix.home.recentvisits.interactor.RecentVisitsInteractor
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * View holder for the "Recent visits" section header with the "Show all" button.
 *
 * @property interactor [RecentVisitsInteractor] which will have delegated to all user
 * interactions.
 */
class RecentVisitsHeaderViewHolder(
    composeView: ComposeView,
    viewLifecycleOwner: LifecycleOwner,
    private val interactor: RecentVisitsInteractor
) : ComposeViewHolder(composeView, viewLifecycleOwner) {

    init {
        val horizontalPadding =
            composeView.resources.getDimensionPixelSize(R.dimen.home_item_horizontal_margin)
        composeView.setPadding(horizontalPadding, 0, horizontalPadding, 0)
    }

    @Composable
    override fun Content() {
        Column {
            Spacer(modifier = Modifier.height(40.dp))

            RecentVisitsHeader(
                text = stringResource(R.string.history_metadata_header_2),
                onShowAllClick = interactor::onHistoryShowAllClicked,
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    companion object {
        val LAYOUT_ID = View.generateViewId()
    }
}

/**
 * Recent visits homepage header.
 *
 * @param text The header string.
 * @param (optional) onShowAllClick Action to take when show all is clicked.
 */
@Composable
fun RecentVisitsHeader(
    text: String,
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

        val description = stringResource(id = R.string.recent_tabs_show_all)
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

@Composable
@Preview
private fun RecentVisitsHeaderViewHolderPreview() {
    FirefoxTheme {
        RecentVisitsHeader(
            stringResource(R.string.history_metadata_header_2)
        )
    }
}
