/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.theme

import androidx.compose.ui.graphics.Color

/**
 * All standardized colors for Firefox applications.
 *
 * For a consistent UX all composables should use these colors only.
 *
 * [v 0.1.0 from September 8 2021](https://www.figma.com/file/DyIIHvRgqt2EXfAVn1gV9c/Fx-Colors)
 */
@Suppress("MagicNumber")
object FirefoxColors {
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)

    // Light grey should primarily be used for the Light Theme and secondary buttons.
    val LightGrey05 = Color(0xFFFBFBFE)
    val LightGrey10 = Color(0xFFF9F9FB)
    val LightGrey20 = Color(0xFFF0F0F4)
    val LightGrey30 = Color(0xFFE0E0E6)
    val LightGrey40 = Color(0xFFCFCFD8)
    val LightGrey50 = Color(0xFFBFBFC9)
    val LightGrey60 = Color(0xFFAFAFBA)
    val LightGrey70 = Color(0xFF9F9FAD)
    val LightGrey80 = Color(0xFF8F8F9D)
    val LightGrey90 = Color(0xFF80808E)

    // Dark grey should primarily be used for the Dark theme and secondary buttons.
    val DarkGrey05 = Color(0xFF5B5B66)
    val DarkGrey10 = Color(0xFF52525E)
    val DarkGrey20 = Color(0xFF4A4A55)
    val DarkGrey30 = Color(0xFF42414D)
    val DarkGrey40 = Color(0xFF3A3944)
    val DarkGrey50 = Color(0xFF32313C)
    val DarkGrey60 = Color(0xFF2B2A33)
    val DarkGrey70 = Color(0xFF23222B)
    val DarkGrey80 = Color(0xFF1C1B22)
    val DarkGrey90 = Color(0xFF15141A)

    // Blue is one of our primary accent colors. Used for primary buttons and text links.
    val Blue05 = Color(0xFFAAF2FF)
    val Blue10 = Color(0xFF80EBFF)
    val Blue20 = Color(0xFF00DDFF)
    val Blue30 = Color(0xFF00B3F4)
    val Blue40 = Color(0xFF0090ED)
    val Blue50 = Color(0xFF0060DF)
    val Blue60 = Color(0xFF0250BB)
    val Blue70 = Color(0xFF054096)
    val Blue80 = Color(0xFF073072)
    val Blue90 = Color(0xFF09204D)

    // Green is primarily used for positive / success / update status in user interface.
    val Green05 = Color(0xFFE3FFF3)
    val Green10 = Color(0xFFD1FEEE)
    val Green20 = Color(0xFFB3FFE3)
    val Green30 = Color(0xFF87FFD1)
    val Green40 = Color(0xFF54FFBD)
    val Green50 = Color(0xFF3FE1B0)
    val Green60 = Color(0xFF2AC3A2)
    val Green70 = Color(0xFF008787)
    val Green80 = Color(0xFF005E5E)
    val Green90 = Color(0xFF08403F)

    // Yellow is primarily used for attention / caution / warining status in user interface.
    val Yellow05 = Color(0xFFFFFFCC)
    val Yellow10 = Color(0xFFFFFF98)
    val Yellow20 = Color(0xFFFFEA80)
    val Yellow30 = Color(0xFFFFD567)
    val Yellow40 = Color(0xFFFFBD4F)
    val Yellow50 = Color(0xFFFFA436)
    val Yellow60 = Color(0xFFE27F2E)
    val Yellow70 = Color(0xFFC45A27)
    val Yellow80 = Color(0xFFA7341F)
    val Yellow90 = Color(0xFF960E18)

    // Firefox Orange is only used for branding. Do not use it otherwise!
    val Orange05 = Color(0xFFFFF4DE)
    val Orange10 = Color(0xFFFFD5B2)
    val Orange20 = Color(0xFFFFB587)
    val Orange30 = Color(0xFFFFA266)
    val Orange40 = Color(0xFFFF8A50)
    val Orange50 = Color(0xFFFF7139)
    val Orange60 = Color(0xFFE25920)
    val Orange70 = Color(0xFFCC3D00)
    val Orange80 = Color(0xFF9E280B)
    val Orange90 = Color(0xFF7C1504)

    // Red is primarily used for attention / error status or a destructive action in user interface.
    val Red05 = Color(0xFFFFDFE7)
    val Red10 = Color(0xFFFFBDC5)
    val Red20 = Color(0xFFFF9AA2)
    val Red30 = Color(0xFFFF848B)
    val Red40 = Color(0xFFFF6A75)
    val Red50 = Color(0xFFFF4F5E)
    val Red60 = Color(0xFFE22850)
    val Red70 = Color(0xFFC50042)
    val Red80 = Color(0xFF810220)
    val Red90 = Color(0xFF440306)

    // Violet is primarily used for ????? in user interface.
    val Violet05 = Color(0xFFE7DFFF)
    val Violet10 = Color(0xFFD9BFFF)
    val Violet20 = Color(0xFFCB9EFF)
    val Violet30 = Color(0xFFC689FF)
    val Violet40 = Color(0xFFAB71FF)
    val Violet50 = Color(0xFF9059FF)
    val Violet60 = Color(0xFF7542E5)
    val Violet70 = Color(0xFF592ACB)
    val Violet80 = Color(0xFF45278D)
    val Violet90 = Color(0xFF321C64)

    // Purple is commonly used to indicate privacy.
    val Purple05 = Color(0xFFF7E2FF)
    val Purple10 = Color(0xFFF6B8FF)
    val Purple20 = Color(0xFFF68FFF)
    val Purple30 = Color(0xFFF770FF)
    val Purple40 = Color(0xFFD74CF0)
    val Purple50 = Color(0xFFB833E1)
    val Purple60 = Color(0xFF952BB9)
    val Purple70 = Color(0xFF722291)
    val Purple80 = Color(0xFF4E1A69)
    val Purple90 = Color(0xFF2B1141)

    // Firefox Ink is commonly used for Firefox branding and new product websites.
    val Ink05 = Color(0xFF393473)
    val Ink10 = Color(0xFF342F6D)
    val Ink20 = Color(0xFF312A64)
    val Ink30 = Color(0xFF2E255D)
    val Ink40 = Color(0xFF2B2156)
    val Ink50 = Color(0xFF291D4F)
    val Ink60 = Color(0xFF271948)
    val Ink70 = Color(0xFF241541)
    val Ink80 = Color(0xFF20123A)
    val Ink90 = Color(0xFF1D1133)
}
