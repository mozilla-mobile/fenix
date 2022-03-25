/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.compose

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonElevation
import androidx.compose.material.Icon
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.mozilla.fenix.R
import org.mozilla.fenix.R.font
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * Default [Button] to be shown for neutral actions.
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
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 * this button in different states. See [ButtonDefaults.buttonColors].
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
fun NeutralButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ButtonElevation? = ButtonDefaults.elevation(),
    colors: ButtonColors = ButtonDefaults.buttonColors(
        backgroundColor = FirefoxTheme.colors.actionSecondary,
        contentColor = FirefoxTheme.colors.textActionSecondary
    ),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    arrangement: Arrangement.Horizontal = Arrangement.Center,
    maxLines: Int = 1,
    iconsPadding: Dp = 8.dp,
    iconsTint: Color = FirefoxTheme.colors.iconActionSecondary,
    @DrawableRes leadingIconRes: Int? = null,
    @DrawableRes trailingIconRes: Int? = null,
) {
    CompositionLocalProvider(LocalRippleTheme provides NeutralButtonRippleTheme) {
        Button(
            onClick = onClick,
            modifier = modifier.then(
                Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .height(48.dp)
                    .padding(vertical = 6.dp) // default insetTop/insetBottom value of a MaterialButton
                    .fillMaxWidth()
            ),
            interactionSource = interactionSource,
            elevation = elevation,
            colors = colors,
            contentPadding = contentPadding
        ) {
            TextWithIcons(
                text = text,
                textColor = colors.contentColor(true).value,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(font.metropolis_semibold)),
                letterSpacing = 0.sp,
                maxLines = maxLines,
                arrangement = arrangement,
                iconsPadding = iconsPadding,
                leadingIcon = {
                    leadingIconRes?.let {
                        Icon(
                            painter = painterResource(id = it),
                            tint = iconsTint,
                            contentDescription = null
                        )
                    }
                },
                trailingIcon = {
                    trailingIconRes?.let {
                        Icon(
                            painter = painterResource(id = it),
                            tint = iconsTint,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    }
}

private object NeutralButtonRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor() = FirefoxTheme.colors.textSecondary

    @Composable
    override fun rippleAlpha() =
        RippleTheme.defaultRippleAlpha(
            Color.Black,
            lightTheme = !isSystemInDarkTheme()
        )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NeutralButtonDarkThemePreview() {
    FirefoxTheme {
        NeutralButton(
            text = "This is a test for dark theme",
            onClick = {}
        )
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun NeutralButtonLightThemePreview() {
    FirefoxTheme {
        NeutralButton(
            text = "This is a test for light theme",
            onClick = {},
            leadingIconRes = R.drawable.ic_firefox
        )
    }
}
