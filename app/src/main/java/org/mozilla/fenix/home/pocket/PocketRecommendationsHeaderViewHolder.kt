/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.pocket

import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.fenix.R
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * [RecyclerView.ViewHolder] for displaying the Pocket feature header.
 *
 * @param composeView [ComposeView] which will be populated with Jetpack Compose UI content.
 * @param interactor [PocketStoriesInteractor] callback for user interaction.
 */
class PocketRecommendationsHeaderViewHolder(
    composeView: ComposeView,
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
            FirefoxTheme {
                PoweredByPocketHeader(
                    interactor::onLearnMoreClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 24.dp,
                            start = horizontalPadding.dp,
                            end = horizontalPadding.dp
                        )
                )
            }
        }
    }

    companion object {
        val LAYOUT_ID = View.generateViewId()
    }
}

@Composable
@Preview
fun PocketRecommendationsFooterViewHolderPreview() {
    FirefoxTheme {
        PoweredByPocketHeader({ })
    }
}
