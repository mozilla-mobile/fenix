/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.util.TypedValue
import android.view.View
import android.view.Window
import androidx.core.content.ContextCompat

interface ThemeManager {
    enum class Theme {
        Normal, Private
    }

    val currentTheme: Theme
    fun setTheme(theme: Theme)
}

fun Activity.setTheme(theme: ThemeManager.Theme) {
    val themeCode = when (theme) {
        ThemeManager.Theme.Normal -> R.style.NormalTheme
        ThemeManager.Theme.Private -> R.style.PrivateTheme
    }

    setTheme(themeCode)
}

fun ThemeManager.Theme.isPrivate(): Boolean = this == ThemeManager.Theme.Private

class DefaultThemeManager : ThemeManager {
    var temporaryThemeManagerStorage = ThemeManager.Theme.Normal

    var onThemeChange: ((ThemeManager.Theme) -> Unit)? = null

    override val currentTheme: ThemeManager.Theme
        get() = temporaryThemeManagerStorage

    override fun setTheme(theme: ThemeManager.Theme) {
        if (temporaryThemeManagerStorage != theme) {
            temporaryThemeManagerStorage = theme

            onThemeChange?.invoke(currentTheme)
        }
    }

    companion object {
        fun resolveAttribute(attribute: Int, context: Context): Int {
            val typedValue = TypedValue()
            val theme = context.theme
            theme.resolveAttribute(attribute, typedValue, true)

            return typedValue.resourceId
        }

        // Handles status bar theme change since the window does not dynamically recreate
        fun applyStatusBarTheme(
            window: Window,
            themeManager: ThemeManager,
            context: Context,
            onHomeScreen: Boolean = true
        ) {
            window.statusBarColor = ContextCompat
                .getColor(
                    context, DefaultThemeManager
                        .resolveAttribute(android.R.attr.statusBarColor, context)
                )

            when (themeManager.currentTheme) {
                ThemeManager.Theme.Normal -> {
                    val currentNightMode =
                        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                    when (currentNightMode) {
                        Configuration.UI_MODE_NIGHT_NO -> {
                            updateLightNavigationBar(onHomeScreen, window, context)
                        }
                        Configuration.UI_MODE_NIGHT_YES -> {
                            window.decorView.systemUiVisibility =
                                window.decorView.systemUiVisibility and
                                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() and
                                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                            updateNavigationBar(onHomeScreen, window, context)
                        }
                        Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                            // We assume light here per Android doc's recommendation
                            updateLightNavigationBar(onHomeScreen, window, context)
                        }
                    }
                }
                ThemeManager.Theme.Private -> {
                    window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and
                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() and
                            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                    updateNavigationBar(onHomeScreen, window, context)
                }
            }
        }

        private fun updateLightNavigationBar(onHomeScreen: Boolean, window: Window, context: Context) {
            if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.O) {
                // API level can display handle light navigation bar color
                updateNavigationBar(onHomeScreen, window, context)
                window.decorView.systemUiVisibility =
                    window.decorView.systemUiVisibility or
                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                window.decorView.systemUiVisibility =
                    window.decorView.systemUiVisibility or
                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }

        private fun updateNavigationBar(onHomeScreen: Boolean, window: Window, context: Context) {
            if (onHomeScreen) {
                window.navigationBarColor = ContextCompat
                    .getColor(
                        context, DefaultThemeManager
                            .resolveAttribute(R.attr.foundation, context)
                    )
            } else {
                window.navigationBarColor = ContextCompat
                    .getColor(
                        context, DefaultThemeManager
                            .resolveAttribute(R.attr.foundation, context)
                    )
            }
        }
    }
}
