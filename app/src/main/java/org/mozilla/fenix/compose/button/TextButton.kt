/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.compose.button

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults.textButtonColors
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * Text-only button.
 *
 * @param modifier [Modifier] Used to shape and position the underlying [androidx.compose.material.TextButton].
 * @param text The button text to be displayed.
 * @param buttonColors [ButtonColors] for the button.
 * @param onClick Invoked when the user clicks on the button.
 * @param textColor [Color] to apply to the button text.
 */
@Composable
@Suppress("LongParameterList")
fun TextButton(
    modifier: Modifier = Modifier,
    text: String,
    buttonColors: ButtonColors = textButtonColors(),
    onClick: () -> Unit,
    textColor: Color = FirefoxTheme.colors.textAccent,
) {
    androidx.compose.material.TextButton(
        onClick = onClick,
        colors = buttonColors,
        modifier = modifier,
    ) {
        Text(
            text = text,
            color = textColor,
            style = FirefoxTheme.typography.button,
            maxLines = 1,
        )
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun TextButtonPreview() {
    FirefoxTheme {
        Box(Modifier.background(FirefoxTheme.colors.layer1)) {
            TextButton(
                text = "label",
                onClick = {},
            )
        }
    }
}
