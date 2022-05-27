/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val FenixTypography = Typography(

//    h1 = , Not currently in design system
//    h2 = , Not currently in design system
//    h3 = , Not currently in design system
//    h4 = , Not currently in design system

    /** Currently not in-use. */
    h5 = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.18.sp,
        lineHeight = 24.sp,
    ),

    /** Used for headings on Onboarding Modals and App Bar Titles. */
    h6 = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.W500,
        letterSpacing = 0.15.sp,
        lineHeight = 24.sp,
    ),

    /** Used for Lists. */
    subtitle1 = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.15.sp,
        lineHeight = 24.sp,
    ),

    /** Currently not in-use. */
    subtitle2 = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.W500,
        letterSpacing = 0.1.sp,
        lineHeight = 24.sp,
    ),

    /** Currently not in-use. */
    body1 = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.5.sp,
        lineHeight = 24.sp,
    ),

    /** Used for body text. */
    body2 = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.25.sp,
        lineHeight = 20.sp,
    ),

    /** Used for Buttons. */
    button = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.W500,
        letterSpacing = 0.25.sp,
        lineHeight = 14.sp,
    ),

    /** Used for captions. */
    caption = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.4.sp,
        lineHeight = 16.sp,
    ),

    /** Used for Sheets. */
    overline = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.W400,
        letterSpacing = 1.5.sp,
        lineHeight = 16.sp,
    ),
)

/** Used for headings on Cards, Dialogs, Banners, and Homepage. */
val Typography.h7: TextStyle
    @Composable
    get() {
        return TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.W500,
            letterSpacing = 0.15.sp,
            lineHeight = 24.sp,
        )
    }

/** Used for Small Headings. */
val Typography.h8: TextStyle
    @Composable
    get() {
        return TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.W500,
            letterSpacing = 0.4.sp,
            lineHeight = 20.sp,
        )
    }

@Composable
@Preview
private fun TypographyPreview() {
    val textStyles = listOf(
//        Pair("Headline 1", MaterialTheme.typography.h1),
//        Pair("Headline 2", MaterialTheme.typography.h2),
//        Pair("Headline 3", MaterialTheme.typography.h3),
//        Pair("Headline 4", MaterialTheme.typography.h4),
        Pair("Headline 5", MaterialTheme.typography.h5),
        Pair("Headline 6", MaterialTheme.typography.h6),
        Pair("Headline 7", MaterialTheme.typography.h7),
        Pair("Headline 8", MaterialTheme.typography.h8),
        Pair("Subtitle1", MaterialTheme.typography.subtitle1),
        Pair("Subtitle2", MaterialTheme.typography.subtitle2),
        Pair("Body1", MaterialTheme.typography.body1),
        Pair("Body2", MaterialTheme.typography.body2),
        Pair("Button", MaterialTheme.typography.button),
        Pair("Caption", MaterialTheme.typography.caption),
        Pair("Overline", MaterialTheme.typography.overline),
    )

    FirefoxTheme(theme = Theme.getTheme(isPrivate = false)) {
        LazyColumn(
            modifier = Modifier
                .background(FirefoxTheme.colors.layer1)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            items(textStyles) { style ->
                Text(
                    text = style.first,
                    style = style.second,
                )
            }
        }
    }
}
