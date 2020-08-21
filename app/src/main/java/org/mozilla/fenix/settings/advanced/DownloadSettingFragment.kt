/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.advanced

import android.os.Bundle
import android.os.Environment
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import org.mozilla.fenix.FeatureFlags
import org.mozilla.fenix.R
import org.mozilla.fenix.components.PrivateShortcutCreateManager
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.metrics
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.settings.RadioButtonInfoPreference
import org.mozilla.fenix.settings.SharedPreferenceUpdater
import org.mozilla.fenix.settings.TrackingProtectionFragmentDirections
import org.mozilla.fenix.settings.requirePreference
import org.mozilla.fenix.utils.view.addToRadioGroup

/**
 * Lets the user customize Download options.
 */
class DownloadSettingFragment : PreferenceFragmentCompat() {

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_downloads))
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.downloads_preferences, rootKey)
        val radioDefault = bindDownloadPathRadio(DownloadPathMode.DEFAULT)
        val radioCustom = bindDownloadPathRadio(DownloadPathMode.CUSTOM)
        requirePreference<EditTextPreference>(R.string.pref_key_custom_path_text).isEnabled =
            radioCustom.isEnabled

        addToRadioGroup(radioDefault, radioCustom)
    }

    private fun bindDownloadPathRadio(
        mode: DownloadPathMode
    ): RadioButtonInfoPreference {
        val radioButton = requirePreference<RadioButtonInfoPreference>(mode.preferenceKey)
        radioButton.contentDescription = getString(mode.contentDescriptionRes)

        radioButton.onClickListener {
            // send telemetry event
            // update mode in engine
            updateDownloadPath(mode)
        }
//        val metrics = requireComponents.analytics.metrics
//        radioButton.onClickListener {
//            when (mode) {
//                DownloadPathMode.DEFAULT ->
//                    Event.DownloadPathSettingChanged.Setting.DEFAULT
//                DownloadPathMode.CUSTOM ->
//                    Event.DownloadPathSettingChanged.Setting.CUSTOM
//            }.let { setting ->
//                metrics.track(Event.DownloadPathSettingChanged(setting))
//            }
//        }

        return radioButton
    }

    private fun updateDownloadPath(
        mode: DownloadPathMode
    ) {
        with(requireComponents.core) {
            when (mode) {
                DownloadPathMode.DEFAULT -> {
                    DOWNLOAD_PATH_DEFAULT
                }
                else -> {
                    requirePreference<EditTextPreference>(R.string.pref_key_custom_path_text).text
                }
            }.let { path ->
                engine.settings.downloadPath = path
            }
        }

    }

    companion object {
        val DOWNLOAD_PATH_DEFAULT: String = Environment.DIRECTORY_DOWNLOADS
    }
}
