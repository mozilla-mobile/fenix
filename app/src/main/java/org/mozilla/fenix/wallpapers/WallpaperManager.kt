/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.wallpapers

import android.content.Context
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import mozilla.components.support.ktx.android.content.getColorFromAttr
import org.mozilla.fenix.ext.asActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.mozilla.fenix.utils.Settings

/**
 * Provides access to available wallpapers and manages their states.
 */
class WallpaperManager(private val settings: Settings) {
    val wallpapers = Wallpaper.values()

    private var _currentWallpaper = MutableStateFlow(Wallpaper.valueOf(settings.currentWallpaper))
    var currentWallpaper: StateFlow<Wallpaper> = _currentWallpaper

    /**
     * Switch the selected wallpaper. This change will be persisted to disk.
     *
     * @param wallpaper The wallpaper to switch to.
     */
    fun updateWallpaperSelection(wallpaper: Wallpaper) {
        settings.currentWallpaper = wallpaper.name
        _currentWallpaper.value = wallpaper
    }

    /**
     * Update the selected wallpaper to be the next wallpaper, as enumerated by [Wallpaper].
     * This change will be persisted to disk.
     */
    fun switchToNextWallpaper() {
        val current = _currentWallpaper.value
        val wallpapers = Wallpaper.values()
        val nextIndex = wallpapers.indexOf(current) + 1
        val nextWallpaper = if (nextIndex >= wallpapers.size) {
            wallpapers.first()
        } else {
            wallpapers[nextIndex]
        }
        updateWallpaperSelection(nextWallpaper)
    }

    /**
     * Apply the [newWallpaper] into the [wallpaperContainer] and update the [currentWallpaper].
     */
    fun applyToView(wallpaperContainer: View, newWallpaper: Wallpaper) {
        if (newWallpaper == Wallpaper.NONE) {
            val context = wallpaperContainer.context
            wallpaperContainer.setBackgroundColor(context.getColorFromAttr(newWallpaper.resource))
        } else {
            wallpaperContainer.setBackgroundResource(newWallpaper.resource)
        }

        adjustTheme(wallpaperContainer.context, newWallpaper)
    }
    private fun adjustTheme(context: Context, newWallpaper: Wallpaper) {
        val mode = if (newWallpaper != Wallpaper.NONE) {
            if (newWallpaper.isDark) {
                updateThemePreference(useDarkTheme = true)
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                updateThemePreference(useLightTheme = true)
                AppCompatDelegate.MODE_NIGHT_NO
            }
        } else {
            updateThemePreference(followDeviceTheme = true)
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode)
            context.asActivity()?.recreate()
        }
    }

    private fun updateThemePreference(
        useDarkTheme: Boolean = false,
        useLightTheme: Boolean = false,
        followDeviceTheme: Boolean = false
    ) {
        settings.shouldUseDarkTheme = useDarkTheme
        settings.shouldUseLightTheme = useLightTheme
        settings.shouldFollowDeviceTheme = followDeviceTheme
    }
}
