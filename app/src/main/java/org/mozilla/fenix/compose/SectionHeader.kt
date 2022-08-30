/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.compose

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import mozilla.components.lib.state.ext.observeAsComposableState
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.components.components
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.wallpapers.Wallpaper
import org.mozilla.fenix.wallpapers.WallpaperState

/**
 * Default layout for the header of a screen section.
 *
 * @param text [String] to be styled as header and displayed.
 * @param modifier [Modifier] to be applied to the [Text].
 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    appStore: AppStore = components.appStore
) {
    val wallpaperState = appStore
        .observeAsComposableState { state -> state.wallpaperState }.value

    val wallpaperAdaptedTextColor = wallpaperState?.currentWallpaper?.textColor?.let { Color(it) }

    SimpleHeader(
        text = text,
        modifier = modifier,
        color = wallpaperAdaptedTextColor ?: FirefoxTheme.colors.textPrimary,
    )
}

@Composable
@Preview("with default wallpaper")
private fun DefaultWallpaperHeadingTextPreview() {
    FirefoxTheme {
        SectionHeader(text = "Section title", appStore = AppStore())
    }
}

@Composable
@Preview("with custom wallpaper", showBackground = true, backgroundColor = 0xffffff)
private fun CustomWallpaperHeadingTextPreview() {
    FirefoxTheme {
        SectionHeader(text = "Section title", appStore = provideCustomWallpaperAppStore())
    }
}

private fun provideCustomWallpaperAppStore(): AppStore {
    return AppStore(
        AppState(
            wallpaperState = WallpaperState(
                Wallpaper(
                    "custom",
                    Wallpaper.DefaultCollection.copy(name = "custom collection"),
                    @Suppress("MagicNumber") // a color is not a magic number.
                    Color(0xFFBB86FC).toArgb().toLong(),
                    0
                ),
                emptyList()
            )
        )
    )
}
