/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TopLevelPropertyNaming")

package org.mozilla.fenix.compose

import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.Dp
import org.mozilla.fenix.R

// Standard paddings. Same as in compose's `TextFieldImpl`.
private val TextHorizontalPadding = 16.dp
private val IconHorizontalPadding = 12.dp

// Tags for identifying the components in parent. Used in the layout phase.
private const val LeadingId = "Leading"
private const val TrailingId = "Trailing"
private const val TextId = "Text"

/**
 * [Text] composable with support for [leadingIcon] and [trailingIcon] bringing to compose
 * [TextView]'s support for `startDrawable` and `endDrawable`.
 *
 * @param text The text to be displayed.
 * @param modifier [Modifier] to apply to this layout node.
 * @param textColor [Color] to apply to the text.
 * If [Color.Unspecified], and [style] has no color set, this will be [LocalContentColor].
 * This color will not affect the icons which can have their own color/tint already applied.
 * @param fontSize The size of glyphs to use when painting the text. See [TextStyle.fontSize].
 * @param fontStyle The typeface variant to use when drawing the letters (e.g., italic).
 * See [TextStyle.fontStyle].
 * @param fontWeight The typeface thickness to use when painting the text (e.g., [FontWeight.Bold]).
 * @param fontFamily The font family to be used when rendering the text. See [TextStyle.fontFamily].
 * @param letterSpacing The amount of space to add between each letter.
 * See [TextStyle.letterSpacing].
 * @param textDecoration The decorations to paint on the text (e.g., an underline).
 * See [TextStyle.textDecoration].
 * @param textAlign The alignment of the text within the lines of the paragraph.
 * See [TextStyle.textAlign].
 * @param lineHeight Line height for the [Paragraph] in [TextUnit] unit, e.g. SP or EM.
 * See [TextStyle.lineHeight].
 * @param overflow How visual overflow should be handled.
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the
 * text will be positioned as if there was unlimited horizontal space. If [softWrap] is false,
 * [overflow] and TextAlign may have unexpected effects.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated according to
 * [overflow] and [softWrap]. If it is not null, then it must be greater than zero.
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A
 * [TextLayoutResult] object that callback provides contains paragraph information, size of the
 * text, baselines and other details. The callback can be used to add additional decoration or
 * functionality to the text. For example, to draw selection around the text.
 * @param textStyle Style configuration for the text such as color, font, line height etc.
 * @param arrangement Used to specify the horizontal arrangement of the text and icons.
 * @param iconsPadding Horizontal space between icons and text. Defaults to 4.dp.
 * @param leadingIcon [Composable] to be shown at the start of the text.
 * @param trailingIcon [Composable] to be shown at the end of the text.
 */
@Composable
@Suppress("LongParameterList", "LongMethod")
fun TextWithIcons(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    textStyle: TextStyle = LocalTextStyle.current,
    arrangement: Arrangement.Horizontal = Arrangement.Center,
    iconsPadding: Dp = TextHorizontalPadding - IconHorizontalPadding,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val currentLayoutDirection = LocalLayoutDirection.current

    Layout(
        content = {
            if (leadingIcon != null) {
                Box(
                    modifier = Modifier.layoutId(LeadingId)
                ) {
                    leadingIcon()
                }
            }

            // Ensure standard padding between icons and text
            val padding = Modifier.padding(
                start = if (leadingIcon != null) iconsPadding else 0.dp,
                end = if (trailingIcon != null) iconsPadding else 0.dp
            )

            Box(
                modifier = Modifier
                    .layoutId(TextId)
                    .then(padding),
                propagateMinConstraints = true
            ) {
                // Instantiating the platform Text
                Text(
                    text = text,
                    modifier = modifier,
                    color = textColor,
                    fontSize = fontSize,
                    fontStyle = fontStyle,
                    fontWeight = fontWeight,
                    fontFamily = fontFamily,
                    letterSpacing = letterSpacing,
                    textDecoration = textDecoration,
                    textAlign = textAlign,
                    lineHeight = lineHeight,
                    overflow = overflow,
                    softWrap = softWrap,
                    maxLines = maxLines,
                    onTextLayout = onTextLayout,
                    style = textStyle
                )
            }

            if (trailingIcon != null) {
                Box(
                    modifier = Modifier.layoutId(TrailingId)
                ) {
                    trailingIcon()
                }
            }
        },
        modifier = modifier,
    ) { items, constraints ->
        var occupiedSpaceHorizontally = 0
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        // Measure leading icon
        val leadingPlaceable =
            items.find { it.layoutId == LeadingId }?.measure(looseConstraints)
        occupiedSpaceHorizontally += widthOrZero(leadingPlaceable)

        // Measure trailing icon
        val trailingPlaceable =
            items.find { it.layoutId == TrailingId }?.measure(looseConstraints)
        occupiedSpaceHorizontally += widthOrZero(trailingPlaceable)

        // Measure text
        val textConstraints = constraints
            .copy(minHeight = 0)
            .offset(
                horizontal = -occupiedSpaceHorizontally
            )
        val textPlaceable =
            items.find { it.layoutId == TextId }!!.measure(textConstraints)

        // Actually lay out placeables.
        val layoutHeight = listOf(leadingPlaceable, textPlaceable, trailingPlaceable)
            .mapNotNull { it?.height }
            .maxOrNull()
        layout(constraints.maxWidth, layoutHeight!!) {
            val itemsPositions = IntArray(items.size)
            val placeables = listOfNotNull(leadingPlaceable, textPlaceable, trailingPlaceable)
            with(arrangement) {
                arrange(
                    constraints.maxWidth,
                    placeables.map { it.width }.toIntArray(),
                    currentLayoutDirection,
                    itemsPositions
                )
            }

            placeables.forEachIndexed { index, item ->
                item.place(itemsPositions[index], (layoutHeight - placeables[index].height) / 2)
            }
        }
    }
}

/**
 * Get [placeable]'s width or `0` if the placeable is null.
 */
private fun widthOrZero(placeable: Placeable?) = placeable?.width ?: 0

@Composable
@Preview
private fun IconsTextPreview() {
    Column {
        Box(modifier = Modifier.background(Color.White)) {
            TextWithIcons(
                text = "This is just a test without using icons",
            )
        }
        Box(modifier = Modifier.background(Color.Cyan)) {
            TextWithIcons(
                text = "This is a test for a leading icon",
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_cookies_enabled),
                        contentDescription = null
                    )
                }
            )
        }
        Box(modifier = Modifier.background(Color.Blue)) {
            TextWithIcons(
                text = "This is a test for both leading and trailing icons",
                textColor = Color.White,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_cookies_enabled),
                        contentDescription = null,
                        tint = Color.White
                    )
                },
                trailingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_cookies_disabled),
                        contentDescription = null,
                        tint = Companion.Red
                    )
                }
            )
        }
    }
}
