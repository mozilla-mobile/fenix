/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.advanced

import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.android.synthetic.main.fragment_download_preferences.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mozilla.fenix.FeatureFlags
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.settings.requirePreference
import java.io.File

/**
 * Lets the user customize Download options.
 */
class DownloadSettingFragment : PreferenceFragmentCompat() {

//    private val engine = requireComponents.core.engine

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_downloads))
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.downloads_preferences, rootKey)
        bindPrefs()
    }

    private fun bindPrefs() {
        val downloadPath = requirePreference<EditTextPreference>(R.string.pref_key_download_path)
        downloadPath.isVisible = FeatureFlags.customDownloadPath
        getFullPath(requireContext().settings().downloadPath).let {
            downloadPath.summary = it
            downloadPath.text = it
        }

        downloadPath.setOnPreferenceChangeListener{ preference, newPath ->
            setDownloadPath(preference, newPath.toString())
            true
        }

        val preferenceExternalDownloadManager =
            requirePreference<Preference>(R.string.pref_key_external_download_manager)
        preferenceExternalDownloadManager.isVisible = FeatureFlags.externalDownloadManager
        //File("").e
    }

    @Suppress("DEPRECATION")
    private fun getFullPath(path: String): String {
        return Environment.getExternalStoragePublicDirectory(path).path
    }

    private fun setDownloadPath(preference: Preference, path: String) {
        preference.context.settings().preferences.edit().putString(preference.key, path).apply()
            requireComponents.core.engine.settings.downloadPath = path

    }
}
