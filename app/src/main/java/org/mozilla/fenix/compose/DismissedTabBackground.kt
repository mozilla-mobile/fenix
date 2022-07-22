/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.DismissDirection
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mozilla.components.feature.tab.collections.Tab
import org.mozilla.fenix.R
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * Composable used to display the background of a [Tab] shown in collections that is being swiped left or right.
 *
 * @param dismissDirection [DismissDirection] of the tab being swiped depending on which this composable
 * will also indicate the swipe direction by placing a warning icon at the start of the swipe gesture.
 * If `null` the warning icon will be shown at both ends.
 * @param shape Whether the tab is to be shown between others or as the last one in collection.
 */
@Composable
fun DismissedTabBackground(
    dismissDirection: DismissDirection?,
    shape: Shape = RectangleShape
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        backgroundColor = FirefoxTheme.colors.layer3,
        shape = shape,
        elevation = 0.dp,
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    // Only show the delete icon for where the swipe starts.
                    .alpha(
                        if (dismissDirection == DismissDirection.StartToEnd) 1f else 0f
                    ),
                tint = FirefoxTheme.colors.iconWarning,
            )

            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    // Only show the delete icon for where the swipe starts.
                    .alpha(
                        if (dismissDirection == DismissDirection.EndToStart) 1f else 0f
                    ),
                tint = FirefoxTheme.colors.iconWarning,
            )
        }
    }
}
