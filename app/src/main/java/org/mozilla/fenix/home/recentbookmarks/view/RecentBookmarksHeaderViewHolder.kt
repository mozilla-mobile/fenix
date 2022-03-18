/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.recentbookmarks.view

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
import org.mozilla.fenix.home.recentbookmarks.interactor.RecentBookmarksInteractor
import org.mozilla.fenix.home.recentvisits.view.HomeSectionHeader
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * View holder for the recent bookmarks header and "Show all" button.
 *
 * @param view The container [View] for this view holder.
 * @param interactor [RecentBookmarksInteractor] which will have delegated to all user interactions.
 */
class RecentBookmarksHeaderViewHolder(
    composeView: ComposeView,
    viewLifecycleOwner: LifecycleOwner,
    private val interactor: RecentBookmarksInteractor
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
        interactor.onShowAllBookmarksClicked()
    }

    @Composable
    override fun Content() {
        Column {
            Spacer(modifier = Modifier.height(40.dp))

            HomeSectionHeader(
                text = stringResource(R.string.recent_bookmarks_title),
                description = stringResource(id = R.string.recently_saved_show_all_content_description_2),
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
private fun RecentBookmarksHeaderPreview() {
    FirefoxTheme {
        HomeSectionHeader(
            stringResource(R.string.recent_bookmarks_title),
            stringResource(id = R.string.recently_saved_show_all_content_description_2),
        )
    }
}
