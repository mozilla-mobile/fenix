/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.compose

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * High level element for displaying Text based on the Primary text style.
 *
 * @param text The text to be displayed.
 * @param modifier [Modifier] to apply to this layout node.
 * @param fontSize The size of glyphs to use when painting the text.
 * @param fontFamily The font family to be used when rendering the text.
 * @param letterSpacing The amount of space to add between each letter.
 * @param overflow How visual overflow should be handled.
 * @param maxLines An optional maximum number of lines for the text to span.
 */
@Suppress("LongParameterList")
@Composable
fun PrimaryText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = text,
        modifier = modifier,
        color = FirefoxTheme.colors.textPrimary,
        fontSize = fontSize,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        overflow = overflow,
        maxLines = maxLines,
    )
}

/**
 * High level element for displaying Text based on the Secondary text style.
 *
 * @param text The text to be displayed.
 * @param modifier [Modifier] to apply to this layout node.
 * @param fontSize The size of glyphs to use when painting the text.
 * @param fontFamily The font family to be used when rendering the text.
 * @param letterSpacing The amount of space to add between each letter.
 * @param overflow How visual overflow should be handled.
 * @param maxLines An optional maximum number of lines for the text to span.
 */
@Suppress("LongParameterList")
@Composable
fun SecondaryText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        color = FirefoxTheme.colors.textSecondary,
        fontSize = fontSize,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        overflow = overflow,
        maxLines = maxLines
    )
}
