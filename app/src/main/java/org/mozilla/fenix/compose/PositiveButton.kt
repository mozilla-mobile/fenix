/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.compose

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.mozilla.fenix.R
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * Default [Button] to be shown for positive actions.
 *
 * @param text The text to be displayed.
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Button in different [Interaction]s.
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 * states. This controls the size of the shadow below the button. Pass `null` here to disable
 * elevation for this button. See [ButtonDefaults.elevation].
 * @param contentPadding The spacing values to apply internally between the container and the content
 * @param arrangement Used to specify the horizontal arrangement of the text and icons.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated according to
 * [overflow] and [softWrap]. If it is not null, then it must be greater than zero.
 * @param iconsPadding Horizontal space between icons and text. Defaults to 4.dp.
 * @param iconsTint Optional tint to be applied to the leading and trailing icons.
 * @param leadingIcon Icon to be shown at the start of the text.
 * @param trailingIcon Icon to be shown at the end of the text.
 */
@Composable
@Suppress("LongParameterList")
fun PositiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ButtonElevation? = ButtonDefaults.elevation(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    arrangement: Arrangement.Horizontal = Arrangement.Center,
    maxLines: Int = 1,
    iconsPadding: Dp = 8.dp,
    iconsTint: Color = FirefoxTheme.colors.iconOnColor,
    @DrawableRes leadingIconRes: Int? = null,
    @DrawableRes trailingIconRes: Int? = null,
) {
    NeutralButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        interactionSource = interactionSource,
        elevation = elevation,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = FirefoxTheme.colors.actionPrimary,
            contentColor = FirefoxTheme.colors.textActionPrimary
        ),
        contentPadding = contentPadding,
        arrangement = arrangement,
        maxLines = maxLines,
        iconsPadding = iconsPadding,
        iconsTint = iconsTint,
        leadingIconRes = leadingIconRes,
        trailingIconRes = trailingIconRes
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun PositiveButtonDarkThemePreview() {
    FirefoxTheme {
        PositiveButton(
            text = "This is a test for dark theme",
            onClick = {}
        )
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun PositiveButtonLightThemePreview() {
    FirefoxTheme {
        PositiveButton(
            text = "This is a test for light theme",
            onClick = {},
            leadingIconRes = R.drawable.ic_firefox
        )
    }
}
