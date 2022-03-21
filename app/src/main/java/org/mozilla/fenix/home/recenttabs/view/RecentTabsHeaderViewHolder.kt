/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.recenttabs.view

import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.findNavController
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.ComposeViewHolder
import org.mozilla.fenix.compose.home.HomeSectionHeader
import org.mozilla.fenix.home.recenttabs.interactor.RecentTabInteractor
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * View holder for the recent tabs header and "Show all" button.
 *
 * @param interactor [RecentTabInteractor] which will have delegated to all user interactions.
 */
class RecentTabsHeaderViewHolder(
    composeView: ComposeView,
    viewLifecycleOwner: LifecycleOwner,
    private val interactor: RecentTabInteractor
) : ComposeViewHolder(composeView, viewLifecycleOwner) {

    init {
        val horizontalPadding =
            composeView.resources.getDimensionPixelSize(R.dimen.home_item_horizontal_margin)
        composeView.setPadding(horizontalPadding, 0, horizontalPadding, 0)
    }

    private fun dismissSearchDialogIfDisplayedAndShowAllClicked() {
        val navController = itemView.findNavController()
        if (navController.currentDestination?.id == R.id.searchDialogFragment) {
            navController.navigateUp()
        }
        interactor.onRecentTabShowAllClicked()
    }

    @Composable
    override fun Content() {
        Column {
            Spacer(modifier = Modifier.height(40.dp))

            HomeSectionHeader(
                text = stringResource(R.string.recent_tabs_header),
                description = stringResource(id = R.string.recent_tabs_show_all_content_description_2),
                onShowAllClick = { dismissSearchDialogIfDisplayedAndShowAllClicked() },
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    companion object {
        val LAYOUT_ID = View.generateViewId()
    }
}

@Composable
@Preview
private fun RecentTabsHeaderPreview() {
    FirefoxTheme {
        HomeSectionHeader(
            stringResource(R.string.recent_tabs_header),
            stringResource(id = R.string.recent_tabs_show_all_content_description_2),
        )
    }
}
