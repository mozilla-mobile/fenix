/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.advanced

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import org.mozilla.fenix.FeatureFlags
import org.mozilla.fenix.R
import org.mozilla.fenix.components.PrivateShortcutCreateManager
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.metrics
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.settings.SharedPreferenceUpdater
import org.mozilla.fenix.settings.requirePreference

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
        updatePreferences()
    }

    private fun updatePreferences() {
        requirePreference<Preference>(R.string.pref_key_download_path).apply {
            setOnPreferenceClickListener {
                requireContext().metrics.track(Event.PrivateBrowsingCreateShortcut)
                PrivateShortcutCreateManager.createPrivateShortcut(requireContext())
                true
            }
        }

        val preferenceExternalDownloadManager =
            requirePreference<SwitchPreference>(R.string.pref_key_external_download_manager).apply {
            onPreferenceChangeListener = SharedPreferenceUpdater()
            isChecked = context.settings().openLinksInAPrivateTab
        }
        preferenceExternalDownloadManager.isVisible = FeatureFlags.externalDownloadManager
    }
}
