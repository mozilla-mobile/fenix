/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.advanced

import android.os.Bundle
import android.os.Environment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.mozilla.fenix.FeatureFlags
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.settings.RadioButtonPreference
import org.mozilla.fenix.settings.requirePreference

/**
 * Lets the user customize Download options.
 *
 *  Scoped storage is introduced in Android 10 (Q) and will become default in
 *  Android 11 (R). Users should only be allowed to use default internal storage or
 *  external/SD storage when available.
 *  See https://developer.android.com/preview/privacy/storage
 */
class DownloadSettingFragment : PreferenceFragmentCompat() {

    private val defaultInternalStorageDirectory: String = Environment.DIRECTORY_DOWNLOADS
    // Passing null will return the root directory for external storage
    // This value will be return null if external storage is not present
    private val externalStorageDirectory: String? =
        requireContext().getExternalFilesDir(null)?.path

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_downloads))
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.downloads_preferences, rootKey)
        bindPrefs()
    }

    private fun bindPrefs() {
        // External download manager
        val preferenceExternalDownloadManager =
            requirePreference<Preference>(R.string.pref_key_external_download_manager)
        preferenceExternalDownloadManager.isVisible = FeatureFlags.externalDownloadManager

        // Download path
        val defaultPath = requirePreference<RadioButtonPreference>(R.string.pref_key_default_download_path)
        val externalStoragePath = requirePreference<RadioButtonPreference>(R.string.pref_key_external_download_path)

        defaultPath.summary = getFullPath(defaultInternalStorageDirectory)

        defaultPath.onClickListener {
            requireContext().settings().preferences.edit()
                .putString(defaultPath.key, defaultInternalStorageDirectory).apply()
            setDownloadPath(defaultInternalStorageDirectory)
        }

        externalStoragePath.isEnabled = isExternalStoragePresent()

        if (externalStoragePath.isEnabled) {
            externalStoragePath.summary =
                getFullPath(externalStorageDirectory ?: defaultInternalStorageDirectory)
        }

        externalStoragePath.onClickListener {
            requireContext().settings().preferences.edit()
                .putString(externalStoragePath.key, externalStorageDirectory).apply()
            setDownloadPath(externalStorageDirectory ?: defaultInternalStorageDirectory)
        }
    }

    @Suppress("DEPRECATION")
    private fun getFullPath(path: String): String {
        return Environment.getExternalStoragePublicDirectory(path).path
    }

    private fun setDownloadPath(path: String) {
        requireComponents.core.engine.settings.downloadPath = path
    }

    private fun isExternalStoragePresent(): Boolean =
        Environment.isExternalStorageRemovable() &&
                (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED)
}
