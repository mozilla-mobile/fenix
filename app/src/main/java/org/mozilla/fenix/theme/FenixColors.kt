/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.theme

import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.mozilla.fenix.browser.browsingmode.BrowsingMode

/**
 * Fenix custom Color Palette.
 */
@Stable
@Suppress("LongParameterList")
class FenixColors(
    /**
     * Above layer color - for transient items or content that floats above the foundation.
     *
     * Potential usages: modal dialogs, menus, sheets and cards.
     */
    above: Color,
    /**
     * Overlay scrim layer color - for adding emphasis for modals sitting in between
     * the foundation and the above layers.
     *
     * Potential usages: share sheet, session management sheet.
     */
    scrim: Color,
    /**
     * Foundation color - for permanent surfaces that comprise the core UI.
     *
     * Potential usages: toolbars, start screen surface and settings surface.
     */
    foundation: Color,

    /**
     * Default text color.
     */
    textPrimary: Color,
    /**
     * Captions, overlines and placeholder texts color.
     */
    textSecondary: Color,
    /**
     * Warnings, errors and deleting texts color.
     */
    textWarnings: Color,

    /**
     * High emphasis buttons text color.
     *
     * Potential usages: the "Save collection" button.
     */
    accent: Color,
    /**
     * High emphasis default color for elements that don't benefit from the weight of a full button.
     *
     * Potential usages: clickable text.
     */
    accentBright: Color,
    /**
     * Low emphasis buttons text color. Applied when otherwise the [accent] color would be too bold.
     *
     * Potential usages: seconary buttons, most commonly found in settings screens or "delete" buttons.
     */
    accentPale: Color,

    /**
     * Whether the app is considered to be in dark theme mode.
     */
    isPrivate: Boolean,
    /**
     * Whether the app is currently in private browsing mode.
     *
     * @see [BrowsingMode]
     */
    isDark: Boolean
) {
    var above by mutableStateOf(above)
        private set
    var scrim by mutableStateOf(scrim)
        private set
    var foundation by mutableStateOf(foundation)
        private set

    var textPrimary by mutableStateOf(textPrimary)
        private set
    var textSecondary by mutableStateOf(textSecondary)
        private set
    var textWarnings by mutableStateOf(textWarnings)
        private set

    var accent by mutableStateOf(accent)
        private set
    var accentBright by mutableStateOf(accentBright)
        private set
    var accentPale by mutableStateOf(accentPale)
        private set

    var isPrivate by mutableStateOf(isPrivate)
        private set
    var isDark by mutableStateOf(isDark)
        private set

    /**
     * Updates the internal values of the given [FenixColors] with values from the [other] [FenixColors]. This
     * allows efficiently updating a subset of [FenixColors], without recomposing every composable that
     * consumes values from [LocalFenixColors].
     *
     * Because [FenixColors] is very wide-reaching, and used by many expensive composables in the
     * hierarchy, providing a new value to [LocalFenixColors] causes every composable consuming
     * [LocalFenixColors] to recompose, which is prohibitively expensive in cases such as animating one
     * color in the theme. Instead, [FenixColors] is internally backed by [mutableStateOf], and this
     * function mutates the internal state of [this] to match values in [other]. This means that any
     * changes will mutate the internal state of [this], and only cause composables that are reading
     * the specific changed value to recompose.
     */
    fun updateColorsFrom(other: FenixColors) {
        above = other.above
        scrim = other.scrim
        foundation = other.foundation
        textPrimary = other.textPrimary
        textSecondary = other.textSecondary
        textWarnings = other.textWarnings
        accent = other.accent
        accentBright = other.accentBright
        accentPale = other.accentPale
        isPrivate = other.isPrivate
        isDark = other.isDark
    }

    /**
     * Returns a copy of this Colors, optionally overriding some of the values.
     */
    fun copy(): FenixColors = FenixColors(
        above = above,
        scrim = scrim,
        foundation = foundation,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        textWarnings = textWarnings,
        accent = accent,
        accentBright = accentBright,
        accentPale = accentPale,
        isPrivate = isPrivate,
        isDark = isDark
    )
}

/**
 * The Fenix light theme is a bright, clean, and slightly cool neutral surface.
 * This emphasizes variable web content.
 */
@Suppress("TopLevelPropertyNaming")
internal val LightColorPalette = FenixColors(
    above = FirefoxColors.White,
    scrim = FirefoxColors.Ink90.copy(alpha = 48f),
    foundation = FirefoxColors.LightGrey10,

    textPrimary = FirefoxColors.DarkGrey90,
    textSecondary = FirefoxColors.DarkGrey05,
    textWarnings = FirefoxColors.Red70,

    accent = FirefoxColors.Ink20,
    accentBright = FirefoxColors.Violet70,
    accentPale = FirefoxColors.LightGrey30,

    isPrivate = false,
    isDark = false
)

/**
 * The Fenix light theme is a dark, clean, and slightly cool neutral surface.
 * This emphasizes variable web content.
 */
@Suppress("TopLevelPropertyNaming")
internal val DarkColorPalette = FenixColors(
    above = FirefoxColors.DarkGrey50,
    scrim = FirefoxColors.Ink90.copy(alpha = 48f),
    foundation = FirefoxColors.DarkGrey80,

    textPrimary = FirefoxColors.LightGrey05,
    textSecondary = FirefoxColors.LightGrey50,
    textWarnings = FirefoxColors.Red30,

    accent = FirefoxColors.Violet50,
    accentBright = FirefoxColors.Violet40,
    accentPale = FirefoxColors.DarkGrey50,

    isPrivate = false,
    isDark = true
)

/**
 * The Fenix light theme is a bold, clean, and slightly cool neutral surface.
 * This emphasizes variable web content.
 */
@Suppress("TopLevelPropertyNaming")
internal val PrivateColorPalette = FenixColors(
    above = FirefoxColors.Ink50,
    scrim = FirefoxColors.Ink90.copy(alpha = 48f),
    foundation = FirefoxColors.Ink90,

    textPrimary = FirefoxColors.LightGrey05,
    textSecondary = FirefoxColors.LightGrey60,
    textWarnings = FirefoxColors.Red30,

    accent = FirefoxColors.Violet50,
    accentBright = FirefoxColors.Violet40,
    accentPale = FirefoxColors.Ink50,

    isPrivate = true,
    isDark = true
)

/**
 * A Material [Colors] implementation which sets all colors to [debugColor] to discourage usage of
 * [MaterialTheme.colors] in preference to [FenixTheme.colors].
 */
internal fun debugColors(
    darkTheme: Boolean,
    debugColor: Color = Color.Red
) = Colors(
    primary = debugColor,
    primaryVariant = debugColor,
    secondary = debugColor,
    secondaryVariant = debugColor,
    background = debugColor,
    surface = debugColor,
    error = debugColor,
    onPrimary = debugColor,
    onSecondary = debugColor,
    onBackground = debugColor,
    onSurface = debugColor,
    onError = debugColor,
    isLight = !darkTheme
)

/**
 * CompositionLocal used to pass [FenixColors] down the tree.
 *
 * Setting the value here is typically done as part of [FenixTheme], which will
 * automatically handle efficiently updating any changed colors without causing unnecessary
 * recompositions, using [FenixColors.updateColorsFrom].
 * To retrieve the current value of this CompositionLocal, use [MaterialTheme.colors].
 */
@Suppress("TopLevelPropertyNaming")
internal val LocalFenixColors = staticCompositionLocalOf<FenixColors> {
    error("No FenixColorPalette provided")
}
