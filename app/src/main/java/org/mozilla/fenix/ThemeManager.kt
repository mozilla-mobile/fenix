/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import android.util.TypedValue

interface ThemeManager {
    enum class Theme {
        Light, Private
    }

    val currentTheme: Theme
    fun setTheme(theme: Theme)
}

fun Activity.setTheme(theme: ThemeManager.Theme) {
    val themeCode = when (theme) {
        ThemeManager.Theme.Light -> R.style.LightTheme
        ThemeManager.Theme.Private -> R.style.PrivateTheme
    }

    setTheme(themeCode)
}


fun ThemeManager.Theme.isPrivate(): Boolean = this == ThemeManager.Theme.Private

private var temporaryThemeManagerStorage = ThemeManager.Theme.Light
class DefaultThemeManager : ThemeManager {
    var onThemeChange: ((ThemeManager.Theme) -> Unit)? = null

    override val currentTheme: ThemeManager.Theme
        get() = temporaryThemeManagerStorage

    override fun setTheme(theme: ThemeManager.Theme) {
        temporaryThemeManagerStorage = theme

        onThemeChange?.invoke(currentTheme)
    }

    companion object {
        fun resolveAttribute(attribute: Int, context: Context): Int {
            val typedValue = TypedValue()
            val theme = context.theme
            theme.resolveAttribute(attribute, typedValue, true)

            return typedValue.resourceId
        }
    }
}
