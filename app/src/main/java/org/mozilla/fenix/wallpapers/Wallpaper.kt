/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.wallpapers

import android.content.Context
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import mozilla.components.support.ktx.android.content.getColorFromAttr
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.asActivity
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.utils.Settings

/**
 * A enum that represents the available wallpapers and their states.
 */
enum class Wallpaper(val resource: Int, val isDark: Boolean) {
    NONE(resource = R.attr.homeBackground, isDark = false),
    FIRST(resource = R.drawable.wallpaper_1, isDark = true),
    SECOND(resource = R.drawable.wallpaper_2, isDark = false);

    val nextWallpaper: Wallpaper get() {
        val wallpapers = values()
        val nextIndex = wallpapers.indexOf(this) + 1
        return if (nextIndex >= wallpapers.size) {
            wallpapers.first()
        } else {
            wallpapers[nextIndex]
        }
    }

    fun applyToView(wallpaperContainer: View) {
        adjustTheme(wallpaperContainer.context, this)
        if (this == NONE) {
            val context = wallpaperContainer.context
            wallpaperContainer.setBackgroundColor(context.getColorFromAttr(this.resource))
        } else {
            wallpaperContainer.setBackgroundResource(this.resource)
        }
    }

    private fun adjustTheme(context: Context, newWallpaper: Wallpaper) {
        val settings = context.components.settings
        val mode = if (newWallpaper != NONE) {
            if (newWallpaper.isDark) {
                updateThemePreference(settings, useDarkTheme = true)
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                updateThemePreference(settings, useLightTheme = true)
                AppCompatDelegate.MODE_NIGHT_NO
            }
        } else {
            updateThemePreference(settings, followDeviceTheme = true)
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode)
            context.asActivity()?.recreate()
        }
    }

    private fun updateThemePreference(
        settings: Settings,
        useDarkTheme: Boolean = false,
        useLightTheme: Boolean = false,
        followDeviceTheme: Boolean = false
    ) {
        settings.shouldUseDarkTheme = useDarkTheme
        settings.shouldUseLightTheme = useLightTheme
        settings.shouldFollowDeviceTheme = followDeviceTheme
    }
}
