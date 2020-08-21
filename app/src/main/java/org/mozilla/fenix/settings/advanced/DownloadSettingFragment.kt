/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.advanced

import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.settings.RadioButtonInfoPreference
import org.mozilla.fenix.settings.requirePreference
import org.mozilla.fenix.settings.search.AddSearchEngineFragment
import org.mozilla.fenix.settings.setOnPreferenceChangeListener
import org.mozilla.fenix.utils.view.addToRadioGroup

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
        val customPathSwitch = requirePreference<Preference>(R.string.pref_key_custom_download_path)
        val pathText = requirePreference<EditTextPreference>(R.string.pref_key_custom_path_text)
        pathText.isEnabled = customPathSwitch.isEnabled

        pathText.setOnPreferenceChangeListener{ preference, _ ->
            val path = pathText.text
            preference.context.settings().preferences.edit().putString(preference.key, path).apply()
//            engine.settings.downloadPath = path
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.add_custom_download_path_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.add_search_engine ->  {
                updateDownloadPath()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun updateDownloadPath() {
//        with(requireComponents.core) {
//            when (mode) {
//                DownloadPathMode.DEFAULT -> {
//                    DOWNLOAD_PATH_DEFAULT
//                }
//                else -> {
//                    requirePreference<EditTextPreference>(R.string.pref_key_custom_path_text).text
//                }
//            }.let { path ->
//                engine.settings.downloadPath = path
//            }
//        }
    }
}
