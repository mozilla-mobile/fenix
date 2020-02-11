/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.annotation.SuppressLint
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceFragmentCompat
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar

/**
 * Lets the user customize the UI.
 */
class CustomizationFragment : PreferenceFragmentCompat() {
    private lateinit var radioLightTheme: RadioButtonPreference
    private lateinit var radioDarkTheme: RadioButtonPreference
    private lateinit var radioAutoBatteryTheme: RadioButtonPreference
    private lateinit var radioFollowDeviceTheme: RadioButtonPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.customization_preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_customize))
        setupPreferences()
    }

    private fun setupPreferences() {
        bindFollowDeviceTheme()
        bindDarkTheme()
        bindLightTheme()
        bindAutoBatteryTheme()
        setupRadioGroups()
        setupToolbarCategory()
    }

    private fun setupRadioGroups() {
        radioLightTheme.addToRadioGroup(radioDarkTheme)

        radioDarkTheme.addToRadioGroup(radioLightTheme)

        if (SDK_INT >= Build.VERSION_CODES.P) {
            radioLightTheme.addToRadioGroup(radioFollowDeviceTheme)
            radioDarkTheme.addToRadioGroup(radioFollowDeviceTheme)

            radioFollowDeviceTheme.addToRadioGroup(radioDarkTheme)
            radioFollowDeviceTheme.addToRadioGroup(radioLightTheme)
        } else {
            radioLightTheme.addToRadioGroup(radioAutoBatteryTheme)
            radioDarkTheme.addToRadioGroup(radioAutoBatteryTheme)

            radioAutoBatteryTheme.addToRadioGroup(radioLightTheme)
            radioAutoBatteryTheme.addToRadioGroup(radioDarkTheme)
        }
    }

    private fun bindLightTheme() {
        val keyLightTheme = getPreferenceKey(R.string.pref_key_light_theme)
        radioLightTheme = requireNotNull(findPreference(keyLightTheme))
        radioLightTheme.onClickListener {
            setNewTheme(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    @SuppressLint("WrongConstant")
    // Suppressing erroneous lint warning about using MODE_NIGHT_AUTO_BATTERY, a likely library bug
    private fun bindAutoBatteryTheme() {
        val keyBatteryTheme = getPreferenceKey(R.string.pref_key_auto_battery_theme)
        radioAutoBatteryTheme = requireNotNull(findPreference(keyBatteryTheme))
        radioAutoBatteryTheme.onClickListener {
            setNewTheme(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
        }
    }

    private fun bindDarkTheme() {
        val keyDarkTheme = getPreferenceKey(R.string.pref_key_dark_theme)
        radioDarkTheme = requireNotNull(findPreference(keyDarkTheme))
        radioDarkTheme.onClickListener {
            requireContext().components.analytics.metrics.track(
                Event.DarkThemeSelected(
                    Event.DarkThemeSelected.Source.SETTINGS
                )
            )
            setNewTheme(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    private fun bindFollowDeviceTheme() {
        val keyDeviceTheme = getPreferenceKey(R.string.pref_key_follow_device_theme)
        radioFollowDeviceTheme = requireNotNull(findPreference(keyDeviceTheme))
        if (SDK_INT >= Build.VERSION_CODES.P) {
            radioFollowDeviceTheme.onClickListener {
                setNewTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    private fun setNewTheme(mode: Int) {
        if (AppCompatDelegate.getDefaultNightMode() == mode) return
        AppCompatDelegate.setDefaultNightMode(mode)
        activity?.recreate()
        with(requireComponents.core) {
            engine.settings.preferredColorScheme = getPreferredColorScheme()
        }
        requireComponents.useCases.sessionUseCases.reload.invoke()
    }

    private fun setupToolbarCategory() {
        val keyToolbarTop = getPreferenceKey(R.string.pref_key_toolbar_top)
        val topPreference = requireNotNull(findPreference<RadioButtonPreference>(keyToolbarTop))
        topPreference.onClickListener {
            requireContext().components.analytics.metrics.track(Event.ToolbarPositionChanged(
                Event.ToolbarPositionChanged.Position.TOP
            ))
        }

        val keyToolbarBottom = getPreferenceKey(R.string.pref_key_toolbar_bottom)
        val bottomPreference = requireNotNull(findPreference<RadioButtonPreference>(keyToolbarBottom))
        bottomPreference.onClickListener {
            requireContext().components.analytics.metrics.track(Event.ToolbarPositionChanged(
                Event.ToolbarPositionChanged.Position.BOTTOM
            ))
        }

        topPreference.setCheckedWithoutClickListener(!requireContext().settings().shouldUseBottomToolbar)
        bottomPreference.setCheckedWithoutClickListener(requireContext().settings().shouldUseBottomToolbar)

        topPreference.addToRadioGroup(bottomPreference)
        bottomPreference.addToRadioGroup(topPreference)
    }
}
