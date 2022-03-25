/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.collections

import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.ComposeViewHolder
import org.mozilla.fenix.compose.PositiveButton
import org.mozilla.fenix.compose.dashedBorder
import org.mozilla.fenix.home.sessioncontrol.CollectionInteractor
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * [RecyclerView.ViewHolder] for displaying a message detailing the collections feature and
 * allowing users to easily start creating their first.
 *
 * @param composeView [ComposeView] which will be populated with Jetpack Compose UI content.
 * @param viewLifecycleOwner [LifecycleOwner] to which this Composable will be tied to.
 * @param interactor [CollectionInteractor] callback for user interaction.
 */
class NoCollectionsMessageViewHolder(
    composeView: ComposeView,
    viewLifecycleOwner: LifecycleOwner,
    private val interactor: CollectionInteractor
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

            NoCollectionsMessage(
                onAddTabsToCollections = interactor::onAddTabsToCollectionTapped,
                onRemoveCollectionsPlaceholder = interactor::onRemoveCollectionsPlaceholder
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    companion object {
        val LAYOUT_ID = View.generateViewId()
    }
}

@Composable
private fun NoCollectionsMessage(
    onAddTabsToCollections: () -> Unit,
    onRemoveCollectionsPlaceholder: () -> Unit
) {
    Column {
        Box(
            modifier = Modifier
                .semantics(mergeDescendants = true) {}
                .dashedBorder(
                    color = FirefoxTheme.colors.borderPrimary,
                    cornerRadius = 8.dp,
                    dashHeight = 2.dp,
                    dashWidth = 4.dp
                )
                .padding(
                    start = 16.dp,
                    bottom = 16.dp
                )
        ) {
            // This Row needs to extend to the very end for the "X" button to have 48dp without breaking the design.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.collections_header),
                    color = FirefoxTheme.colors.textPrimary,
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.metropolis_semibold)),
                    lineHeight = 20.sp
                )

                IconButton(
                    onClick = { onRemoveCollectionsPlaceholder() }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = stringResource(
                            id = R.string.remove_home_collection_placeholder_content_description
                        ),
                        modifier = Modifier.size(24.dp),
                        tint = FirefoxTheme.colors.textPrimary
                    )
                }
            }

            // This Column should be shown with a 16dp horizontal margin for a cohesive design.
            Column(
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = stringResource(id = R.string.no_collections_description2),
                    fontSize = 14.sp,
                    color = FirefoxTheme.colors.textSecondary,
                    letterSpacing = 0.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                PositiveButton(
                    text = stringResource(R.string.tabs_menu_save_to_collection1),
                    onClick = { onAddTabsToCollections() },
                    elevation = ButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    ),
                    contentPadding = PaddingValues(0.dp), // To have space for a bit bigger leading icon
                    iconsPadding = 8.dp,
                    leadingIconRes = R.drawable.ic_tab_collection
                )
            }
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NoCollectionsMessageDarkThemePreview() {
    FirefoxTheme {
        NoCollectionsMessage({}, {})
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun NoCollectionsMessageLightThemePreview() {
    FirefoxTheme {
        NoCollectionsMessage({}, {})
    }
}
