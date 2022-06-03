/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.collections

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissDirection.EndToStart
import androidx.compose.material.DismissDirection.StartToEnd
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import mozilla.components.browser.state.state.recover.RecoverableTab
import mozilla.components.concept.engine.Engine
import mozilla.components.feature.tab.collections.Tab
import org.mozilla.fenix.R.drawable
import org.mozilla.fenix.R.string
import org.mozilla.fenix.compose.list.FaviconListItem
import org.mozilla.fenix.ext.toShortUrl
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.Theme

/**
 * Rectangular shape with only right angles used to display a middle tab.
 */
private val MIDDLE_TAB_SHAPE = RoundedCornerShape(0.dp)

/**
 * Rectangular shape with only the bottom corners rounded used to display the last tab in a collection.
 */
private val BOTTOM_TAB_SHAPE = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)

/**
 * Display an individual [Tab] as part of a collection.
 *
 * @param tab [Tab] to display.
 * @param isLastInCollection Whether the tab is to be shown between others or as the last one in collection.
 * @param onClick Invoked when the user click on the tab.
 * @param onRemove Invoked when the user removes the tab informing also if the tab was swiped to be removed.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CollectionItem(
    tab: Tab,
    isLastInCollection: Boolean,
    onClick: () -> Unit,
    onRemove: (Boolean) -> Unit,
) {
    val dismissState = rememberDismissState()

    if (dismissState.isDismissed(StartToEnd) || dismissState.isDismissed(EndToStart)) {
        onRemove(true)
    }

    SwipeToDismiss(
        state = dismissState,
        background = {
            DismissedTabBackground(
                dismissDirection = dismissState.dismissDirection,
                isLastInCollection = isLastInCollection,
            )
        }
    ) {
        // We need to clip the top bounds to avoid this item drawing shadows over the above item.
        // But we need to add this shadows back to have a clearer separation between tabs
        // when one is being swiped away.
        val clippingModifier by remember {
            derivedStateOf {
                try {
                    if (dismissState.progress.fraction != 1f) Modifier else Modifier.clipTop()
                } catch (e: NoSuchElementException) {
                    // `androidx.compose.material.Swipeable.findBounds` couldn't find anchors.
                    // Happened once in testing when deleting a tab. Could not reproduce afterwards.
                    Modifier.clipTop()
                }
            }
        }

        Card(
            modifier = clippingModifier
                .fillMaxWidth(),
            shape = if (isLastInCollection) BOTTOM_TAB_SHAPE else MIDDLE_TAB_SHAPE,
            backgroundColor = FirefoxTheme.colors.layer2,
            elevation = 5.dp,
        ) {
            FaviconListItem(
                label = tab.title,
                description = tab.url.toShortUrl(),
                onClick = onClick,
                url = tab.url,
                iconPainter = painterResource(drawable.ic_close),
                iconDescription = stringResource(string.remove_tab_from_collection),
                onIconClick = { onRemove(false) },
            )
        }
    }
}

/**
 * Composable used to display the background of a [Tab] shown in collections that is being swiped left or right.
 *
 * @param dismissDirection [DismissDirection] of the tab being swiped depending on which this composable
 * will also indicate the swipe direction by placing a warning icon at the start of the swipe gesture.
 * If `null` the warning icon will be shown at both ends.
 * @param isLastInCollection Whether the tab is to be shown between others or as the last one in collection.
 */
@Composable
private fun DismissedTabBackground(
    dismissDirection: DismissDirection?,
    isLastInCollection: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        backgroundColor = FirefoxTheme.colors.layer3,
        shape = if (isLastInCollection) BOTTOM_TAB_SHAPE else MIDDLE_TAB_SHAPE,
        elevation = 0.dp,
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(drawable.ic_delete),
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    // Only show the delete icon for where the swipe starts.
                    .alpha(
                        if (dismissDirection == StartToEnd) 1f else 0f
                    ),
                tint = FirefoxTheme.colors.iconWarning,
            )

            Icon(
                painter = painterResource(drawable.ic_delete),
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    // Only show the delete icon for where the swipe starts.
                    .alpha(
                        if (dismissDirection == EndToStart) 1f else 0f
                    ),
                tint = FirefoxTheme.colors.iconWarning,
            )
        }
    }
}

/**
 * Clips the Composable this applies to such that it cannot draw content / shadows outside it's top bound.
 */
private fun Modifier.clipTop() = this.then(
    Modifier.drawWithContent {
        val paddingPx = Constraints.Infinity.toFloat()
        clipRect(
            left = 0f - paddingPx,
            top = 0f,
            right = size.width + paddingPx,
            bottom = size.height + paddingPx
        ) {
            this@drawWithContent.drawContent()
        }
    }
)

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun TabInCollectionPreview() {
    FirefoxTheme(theme = Theme.getTheme(isPrivate = false)) {
        Column {
            Box(modifier = Modifier.height(56.dp)) {
                DismissedTabBackground(
                    dismissDirection = StartToEnd,
                    isLastInCollection = false,
                )
            }
            CollectionItem(
                tab = tabPreview,
                isLastInCollection = false,
                onClick = {},
                onRemove = {},
            )

            Spacer(Modifier.height(10.dp))

            Box(modifier = Modifier.height(56.dp)) {
                DismissedTabBackground(
                    dismissDirection = EndToStart,
                    isLastInCollection = true,
                )
            }
            CollectionItem(
                tab = tabPreview,
                isLastInCollection = true,
                onClick = {},
                onRemove = {},
            )
        }
    }
}

private val tabPreview = object : Tab {
    override val id = 2L
    override val title = "Mozilla-Firefox"
    override val url = "https://www.mozilla.org/en-US/firefox/whats-new-in-last-version"

    override fun restore(
        context: Context,
        engine: Engine,
        restoreSessionId: Boolean
    ): RecoverableTab? = null
}
