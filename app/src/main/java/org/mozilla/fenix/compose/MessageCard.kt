/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mozilla.experiments.nimbus.StringHolder
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.button.PrimaryButton
import org.mozilla.fenix.gleanplumb.Message
import org.mozilla.fenix.nimbus.MessageData
import org.mozilla.fenix.nimbus.StyleData
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.theme.Theme

/**
 * Message Card.
 *
 * @param titleText Message title if null not shown
 * @param messageText Message content if null not shown
 * @param buttonText Message button text if null not shown
 * @param onClick Invoked when user clicks on the message card.
 * @param onCloseButtonClick Invoked when user clicks on close button to remove message.
 */
@Suppress("LongMethod")
@Composable
fun MessageCard(
    titleText: String?,
    messageText: String?,
    buttonText: String?,
    onClick: () -> Unit,
    onCloseButtonClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(vertical = 16.dp)
            .then(
                if (buttonText.isNullOrBlank()) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        shape = RoundedCornerShape(8.dp),
        backgroundColor = FirefoxTheme.colors.layer2,
    ) {
        Column(
            Modifier
                .padding(all = 16.dp)
                .fillMaxWidth(),
        ) {
            val title = titleText
            if (!title.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.weight(1f),
                        color = FirefoxTheme.colors.textPrimary,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                        style = FirefoxTheme.typography.headline7,
                    )

                    IconButton(
                        modifier = Modifier.size(20.dp),
                        onClick = onCloseButtonClick,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.mozac_ic_close_20),
                            contentDescription = stringResource(
                                R.string.content_description_close_button,
                            ),
                            tint = FirefoxTheme.colors.iconPrimary,
                        )
                    }
                }

                Text(
                    text = messageText ?: "",
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 14.sp,
                    color = FirefoxTheme.colors.textSecondary,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = messageText ?: "",
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        color = FirefoxTheme.colors.textPrimary,
                    )

                    IconButton(
                        modifier = Modifier.size(20.dp),
                        onClick = onCloseButtonClick,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.mozac_ic_close_20),
                            contentDescription = stringResource(
                                R.string.content_description_close_button,
                            ),
                            tint = FirefoxTheme.colors.iconPrimary,
                        )
                    }
                }
            }

            val buttonLabel = buttonText
            if (!buttonLabel.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))

                PrimaryButton(
                    text = buttonLabel,
                    onClick = onClick,
                )
            }
        }
    }
}

@Composable
@Preview
private fun MessageCardPreview() {
    FirefoxTheme(theme = Theme.getTheme()) {
        Box(Modifier.background(FirefoxTheme.colors.layer1)) {
            MessageCard(
                titleText = stringResource(R.string.bookmark_empty_title_error),
                messageText = stringResource(R.string.default_browser_experiment_card_text),
                buttonText = null,
                onClick = {},
                onCloseButtonClick = {},
            )
        }
    }
}

@Composable
@Preview
private fun MessageCardWithoutTitlePreview() {
    FirefoxTheme(theme = Theme.getTheme()) {
        Box(Modifier.background(FirefoxTheme.colors.layer1)) {
            MessageCard(
                titleText = null,
                messageText = stringResource(R.string.default_browser_experiment_card_text),
                buttonText = null,
                onClick = {},
                onCloseButtonClick = {},
            )
        }
    }
}

@Composable
@Preview
private fun MessageCardWithButtonLabelPreview() {
    FirefoxTheme(theme = Theme.getTheme()) {
        Box(Modifier.background(FirefoxTheme.colors.layer1)) {
            MessageCard(
                titleText = stringResource(R.string.bookmark_empty_title_error),
                messageText = stringResource(R.string.default_browser_experiment_card_text),
                buttonText = stringResource(R.string.preferences_set_as_default_browser),
                onClick = {},
                onCloseButtonClick = {},
            )
        }
    }
}
