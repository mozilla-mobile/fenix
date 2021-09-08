/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.mozilla.fenix.ext.settings

/**
 * Full Fenix Composition theme.
 *
 * The color palette is automatically computed based on the [privateBrowsing] and [darkTheme] parameters.
 * This ensures consistency by not allowing only parts of the color palette to be changed.
 *
 * @param privateBrowsing Whether the app is currently in private browsing mode.
 * @param darkTheme Whether the app is currently in dark theme mode.
 * @param content child [Composable] to be displayed which will have access to this theme.
 */
@Composable
fun FenixTheme(
    privateBrowsing: Boolean = LocalContext.current.settings().lastKnownMode.isPrivate,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = when (privateBrowsing) {
        true -> PrivateColorPalette
        false -> if (darkTheme) DarkColorPalette else LightColorPalette
    }

    ProvideFenixColors(colors) {
        MaterialTheme(
            colors = debugColors(darkTheme),
            content = content
        )
    }
}

/**
 * Contains functions to access the current theme values provided at the call site's position in the hierarchy.
 */
object FenixTheme {
    /**
     * Retrieves the current [Colors] at the call site's position in the hierarchy.
     */
    val colors: FenixColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFenixColors.current
}

/**
 * CompositionLocal used to pass the current color palette down the tree and make it available for the child content.
 *
 * @param colors Color palette which will be made available in the Composition starting from [content].
 * @param content child [Composable] to be displayed which will have access to these colors.
 */
@Composable
fun ProvideFenixColors(
    colors: FenixColors,
    content: @Composable () -> Unit
) {
    val colorPalette = remember {
        // Explicitly creating a new object here so we don't mutate the initial [colors]
        // provided, and overwrite the values set in it.
        colors.copy()
    }
    colorPalette.updateColorsFrom(colors)
    CompositionLocalProvider(LocalFenixColors provides colorPalette, content = content)
}
