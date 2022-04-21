/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.pocket

import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.lib.state.ext.observeAsComposableState
import mozilla.components.service.pocket.PocketRecommendedStory
import org.mozilla.fenix.R
import org.mozilla.fenix.R.string
import org.mozilla.fenix.components.components
import org.mozilla.fenix.compose.ComposeViewHolder
import org.mozilla.fenix.compose.SectionHeader
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.Theme

internal const val POCKET_STORIES_TO_SHOW_COUNT = 8

/**
 * [RecyclerView.ViewHolder] for displaying the list of [PocketRecommendedStory]s from [AppStore].
 *
 * @param composeView [ComposeView] which will be populated with Jetpack Compose UI content.
 * @param viewLifecycleOwner [LifecycleOwner] to which this Composable will be tied to.
 * @param interactor [PocketStoriesInteractor] callback for user interaction.
 */
class PocketStoriesViewHolder(
    composeView: ComposeView,
    viewLifecycleOwner: LifecycleOwner,
    private val interactor: PocketStoriesInteractor
) : ComposeViewHolder(composeView, viewLifecycleOwner) {

    companion object {
        val LAYOUT_ID = View.generateViewId()
    }

    @Composable
    override fun Content() {
        val horizontalPadding = dimensionResource(R.dimen.home_item_horizontal_margin)

        val stories = components.appStore
            .observeAsComposableState { state -> state.pocketStories }.value

        LaunchedEffect(stories) {
            // We should report back when a certain story is actually being displayed.
            // Cannot do it reliably so for now we'll just mass report everything as being displayed.
            stories?.let {
                interactor.onStoriesShown(it)
            }
        }

        Column(modifier = Modifier.padding(top = 72.dp)) {
            SectionHeader(
                text = stringResource(R.string.pocket_stories_header_1),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding)
                    .wrapContentHeight(align = Alignment.Top)
            )

            Spacer(Modifier.height(16.dp))

            PocketStories(
                stories ?: emptyList(),
                horizontalPadding,
                interactor::onStoryClicked,
                interactor::onDiscoverMoreClicked
            )
        }
    }
}

@Composable
@Preview
fun PocketStoriesViewHolderPreview() {
    FirefoxTheme(theme = Theme.getTheme(isPrivate = false)) {
        Column {
            SectionHeader(
                text = stringResource(string.pocket_stories_header_1),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .wrapContentHeight(align = Alignment.Top)
            )

            Spacer(Modifier.height(16.dp))

            PocketStories(
                stories = getFakePocketStories(POCKET_STORIES_TO_SHOW_COUNT),
                contentPadding = 0.dp,
                onStoryClicked = { _, _ -> },
                onDiscoverMoreClicked = {}
            )
        }
    }
}
