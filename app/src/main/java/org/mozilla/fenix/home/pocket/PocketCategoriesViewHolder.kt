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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.lib.state.ext.observeAsComposableState
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.SectionHeader
import org.mozilla.fenix.home.HomeFragmentStore
import org.mozilla.fenix.theme.FirefoxTheme

internal const val POCKET_CATEGORIES_SELECTED_AT_A_TIME_COUNT = 8

/**
 * [RecyclerView.ViewHolder] for displaying the list of [PocketRecommendedStoriesCategory]s from [store].
 *
 * @param composeView [ComposeView] which will be populated with Jetpack Compose UI content.
 * @param store [HomeFragmentStore] containing the list of Pocket stories categories to be displayed.
 * @param interactor [PocketStoriesInteractor] callback for user interaction.
 */
class PocketCategoriesViewHolder(
    composeView: ComposeView,
    private val store: HomeFragmentStore,
    private val interactor: PocketStoriesInteractor
) : RecyclerView.ViewHolder(composeView) {

    init {
        composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )

        composeView.setContent {
            val horizontalPadding = with(composeView.resources) {
                getDimensionPixelSize(R.dimen.home_item_horizontal_margin) / displayMetrics.density
            }

            val categories = store
                .observeAsComposableState { state -> state.pocketStoriesCategories }.value
            val categoriesSelections = store
                .observeAsComposableState { state -> state.pocketStoriesCategoriesSelections }.value

            FirefoxTheme {
                Column(modifier = Modifier.padding(top = 24.dp)) {
                    SectionHeader(
                        text = stringResource(R.string.pocket_stories_categories_header),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPadding.dp)
                            .wrapContentHeight(align = Alignment.Top)
                    )

                    Spacer(Modifier.height(17.dp))

                    PocketStoriesCategories(
                        categories = categories ?: emptyList(),
                        selections = categoriesSelections ?: emptyList(),
                        onCategoryClick = interactor::onCategoryClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPadding.dp)
                    )
                }
            }
        }
    }

    companion object {
        val LAYOUT_ID = View.generateViewId()
    }
}

@Composable
@Preview
private fun PocketCategoriesViewHolderPreview() {
    FirefoxTheme {
        Column(modifier = Modifier.padding(top = 24.dp)) {
            SectionHeader(
                text = stringResource(R.string.pocket_stories_categories_header),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .wrapContentHeight(align = Alignment.Top)
            )

            Spacer(Modifier.height(17.dp))

            @Suppress("UnrememberedMutableState")
            PocketStoriesCategories(
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor".split(" ").map {
                    PocketRecommendedStoriesCategory(it)
                },
                emptyList(),
                { }
            )
        }
    }
}
