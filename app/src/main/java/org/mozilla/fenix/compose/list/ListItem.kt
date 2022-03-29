/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.compose.list

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.Favicon
import org.mozilla.fenix.compose.PrimaryText
import org.mozilla.fenix.compose.SecondaryText
import org.mozilla.fenix.theme.FirefoxTheme

val LIST_ITEM_HEIGHT = 56.dp

/**
 * List item component to be used to display text with an optional second line
 * with the flexibility to add custom UI to either end.
 *
 * @param label The primary text in the item.
 * @param description An optional second line of text.
 * @param onClick Optional lambda for handling clicks.
 * @param beforeListAction Optional Composable for adding UI to the start of the list item.
 * @param afterListAction Optional Composable for adding UI to the end of the list item.
 */
@Composable
fun ListItem(
    label: String,
    description: String? = null,
    onClick: (() -> Unit)? = null,
    beforeListAction: @Composable RowScope.() -> Unit = {},
    afterListAction: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = when (onClick != null) {
            true -> Modifier.clickable { onClick() }
            false -> Modifier
        }.then(
            Modifier.defaultMinSize(minHeight = LIST_ITEM_HEIGHT)
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        beforeListAction()

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .weight(1f),
        ) {
            PrimaryText(
                text = label,
                fontSize = 16.sp,
                maxLines = 1,
            )

            description?.let {
                SecondaryText(
                    text = description,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
        }

        afterListAction()
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TextOnlyListItemPreview() {
    FirefoxTheme {
        Box(Modifier.background(FirefoxTheme.colors.layer1)) {
            ListItem(label = "Label only")
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun ListItemDescriptionPreview() {
    FirefoxTheme {
        Box(Modifier.background(FirefoxTheme.colors.layer1)) {
            ListItem(
                label = "Label + description",
                description = "Description text"
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun ListItemLeftIconPreview() {
    FirefoxTheme {
        Box(Modifier.background(FirefoxTheme.colors.layer1)) {
            ListItem(
                label = "Label + left icon",
                beforeListAction = {
                    Spacer(modifier = Modifier.width(16.dp))

                    Icon(
                        painter = painterResource(R.drawable.ic_folder_icon),
                        contentDescription = "click me",
                        modifier = Modifier.size(24.dp),
                        tint = FirefoxTheme.colors.iconPrimary,
                    )

                    Spacer(modifier = Modifier.width(16.dp))
                }
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun ListItemRightIconPreview() {
    FirefoxTheme {
        Box(Modifier.background(FirefoxTheme.colors.layer1)) {
            ListItem(
                label = "Label + right icon",
                afterListAction = {
                    Icon(
                        painter = painterResource(R.drawable.ic_menu),
                        contentDescription = "click me",
                        modifier = Modifier.size(24.dp),
                        tint = FirefoxTheme.colors.iconPrimary,
                    )

                    Spacer(modifier = Modifier.width(16.dp))
                }
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun ListItemMultiIconPreview() {
    FirefoxTheme {
        Box(Modifier.background(FirefoxTheme.colors.layer1)) {
            ListItem(
                label = "Label + left/right icons",
                beforeListAction = {
                    Spacer(modifier = Modifier.width(16.dp))

                    Icon(
                        painter = painterResource(R.drawable.ic_folder_icon),
                        contentDescription = "click me",
                        modifier = Modifier.size(24.dp),
                        tint = FirefoxTheme.colors.iconPrimary,
                    )

                    Spacer(modifier = Modifier.width(16.dp))
                },
                afterListAction = {
                    Icon(
                        painter = painterResource(R.drawable.ic_menu),
                        contentDescription = "click me",
                        modifier = Modifier.size(24.dp),
                        tint = FirefoxTheme.colors.iconPrimary,
                    )

                    Spacer(modifier = Modifier.width(16.dp))
                }
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun CompleteListItemPreview() {
    FirefoxTheme {
        Box(Modifier.background(FirefoxTheme.colors.layer1)) {
            ListItem(
                label = "Label + left/right icons + clicks",
                description = "Description text",
                onClick = { println("list item click") },
                beforeListAction = {
                    Spacer(modifier = Modifier.width(16.dp))

                    Favicon(
                        url = "",
                        size = 24.dp,
                    )

                    Spacer(modifier = Modifier.width(16.dp))
                },
                afterListAction = {
                    Box(
                        modifier = Modifier
                            .clickable(onClick = { println("menu clicked") })
                            .padding(horizontal = 16.dp)
                            .defaultMinSize(minHeight = LIST_ITEM_HEIGHT),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_menu),
                            contentDescription = "click me",
                            modifier = Modifier.size(24.dp),
                            tint = FirefoxTheme.colors.iconPrimary,
                        )
                    }
                }
            )
        }
    }
}
