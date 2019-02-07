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
import org.jetbrains.anko.defaultSharedPreferences

interface ThemeManager {
    enum class Theme {
        Light, Dark, Private
    }

    fun getCurrentTheme(): Theme
    fun setTheme(theme: Theme, shouldApplyImmediately: Boolean = true)
}

class DefaultThemeManager(private val activity: Activity) : ThemeManager, SharedPreferences.OnSharedPreferenceChangeListener {

    interface ThemeManagerListener {
        fun onThemeChange()
    }

    private val listener: ThemeManagerListener? = null

    override fun getCurrentTheme(): ThemeManager.Theme {
        val isPrivate = PreferenceManager.getDefaultSharedPreferences(activity)
            .getBoolean(activity.getString(R.string.pref_key_private_mode), false)

        if (isPrivate) { return ThemeManager.Theme.Private }

        val currentTheme = PreferenceManager.getDefaultSharedPreferences(activity)
            .getInt(activity.getString(R.string.pref_key_theme), R.style.LightTheme)

        return when (currentTheme) {
            R.style.LightTheme -> ThemeManager.Theme.Light
            else -> ThemeManager.Theme.Dark
        }
    }

    override fun setTheme(theme: ThemeManager.Theme, shouldApplyImmediately: Boolean) {
        val themeCode = when (theme) {
            ThemeManager.Theme.Light -> R.style.LightTheme
            ThemeManager.Theme.Private -> R.style.PrivateTheme
            else -> R.style.LightTheme
        }

        activity.setTheme(themeCode)

        // Do not store the private theme so we remember the user's theme choice after they exit private mode
        if (themeCode != R.style.PrivateTheme) {
            PreferenceManager.getDefaultSharedPreferences(activity)
                .edit().putInt(activity.getString(R.string.pref_key_theme), themeCode).apply()
        }

        if (shouldApplyImmediately) { activity.recreate() }

        // Alert those listening to us
        listener?.onThemeChange()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Log.d("Theme", "key: " + key)
        if (key == activity.getString(R.string.pref_key_private_mode)) {
            setTheme(ThemeManager.Theme.Private)
        }
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
