/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.wallpaper

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mozilla.components.lib.state.ext.observeAsComposableState
import org.mozilla.fenix.R
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.wallpapers.Wallpaper

/**
 * A screen for various settings related to wallpapers.
 *
 * @param wallpaperManager Manager for retrieving and updating current wallpaper.
 */
@Composable
fun WallpaperSettings(
    appStore: AppStore
) {
    val selectedWallpaper = appStore.observeAsComposableState { state -> state.wallpaper }.value
    Surface(color = FirefoxTheme.colors.layer2) {
        WallpaperThumbnails(
            wallpapers = Wallpaper.values().toList(),
            selectedWallpaper = selectedWallpaper ?: Wallpaper.NONE,
            onSelectionChanged = { appStore.dispatch(AppAction.UpdateWallpaper(it)) }
        )
    }
}

/**
 * A grid of selectable wallpaper thumbnails.
 *
 * @param wallpapers Wallpapers to add to grid
 * @param selectedWallpaper The currently selected wallpaper
 * @param numColumns The number of columns that will occupy the grid.
 * @param onSelectionChanged Action to take when a new wallpaper is selected.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WallpaperThumbnails(
    wallpapers: List<Wallpaper>,
    selectedWallpaper: Wallpaper,
    numColumns: Int = 3,
    onSelectionChanged: (Wallpaper) -> Unit,
) {
    LazyVerticalGrid(
        cells = GridCells.Fixed(numColumns),
        modifier = Modifier.padding(vertical = 30.dp, horizontal = 20.dp)
    ) {
        items(wallpapers) { wallpaper ->
            WallpaperThumbnailItem(
                wallpaper = wallpaper,
                isSelected = selectedWallpaper == wallpaper,
                onSelectionChanged = onSelectionChanged
            )
        }
    }
}

/**
 * A single wallpaper thumbnail.
 *
 * @param wallpaper The wallpaper to display.
 * @param isSelected Whether the wallpaper is currently selected.
 * @param aspectRatio The ratio of height to width of the thumbnail
 * @param onSelectionChanged Action to take when wallpaper is selected.
 */
@Composable
private fun WallpaperThumbnailItem(
    wallpaper: Wallpaper,
    isSelected: Boolean,
    aspectRatio: Float = 1.1f,
    onSelectionChanged: (Wallpaper) -> Unit
) {
    val thumbnailShape = RoundedCornerShape(8.dp)
    val border = if (isSelected) {
        Modifier.border(
            BorderStroke(width = 2.dp, color = FirefoxTheme.colors.borderAccent),
            thumbnailShape
        )
    } else {
        Modifier.border(
            BorderStroke(width = 1.dp, color = FirefoxTheme.colors.borderDefault),
            thumbnailShape
        )
    }
    val background = if (wallpaper == Wallpaper.NONE) {
        Modifier.background(color = FirefoxTheme.colors.layer1)
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .padding(4.dp)
            .clip(thumbnailShape)
            .then(background)
            .then(border)
            .clickable { onSelectionChanged(wallpaper) }
    ) {
        if (wallpaper != Wallpaper.NONE) {
            WallpaperImageThumbnail(
                wallpaper = wallpaper,
            )
        }
    }
}

/**
 * A image of a drawable wallpaper.
 *
 * @param wallpaper The wallpaper to use.
 */
@Composable
private fun WallpaperImageThumbnail(wallpaper: Wallpaper) {
    val contentDescription = stringResource(
        R.string.content_description_wallpaper_name, wallpaper.name
    )
    Image(
        painterResource(id = wallpaper.resource),
        contentScale = ContentScale.FillBounds,
        contentDescription = contentDescription,
        modifier = Modifier.fillMaxSize()
    )
}

@Preview
@Composable
private fun WallpaperThumbnailsPreview() {
    WallpaperThumbnails(
        wallpapers = Wallpaper.values().toList(),
        onSelectionChanged = {},
        selectedWallpaper = Wallpaper.NONE
    )
}
