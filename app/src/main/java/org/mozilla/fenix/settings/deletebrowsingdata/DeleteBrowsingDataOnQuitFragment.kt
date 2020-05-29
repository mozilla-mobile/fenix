/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.deletebrowsingdata

import android.os.Bundle
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.settings.BooleanSharedPreferenceUpdater

class DeleteBrowsingDataOnQuitFragment : PreferenceFragmentCompat() {

    private val checkboxes by lazy {
        val context = requireContext()
        DeleteBrowsingDataOnQuitType.values()
            .asSequence()
            .mapNotNull { type ->
                findPreference<CheckBoxPreference>(type.getPreferenceKey(context))?.let { pref ->
                    type to pref
                }
            }
            .toMap()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.delete_browsing_data_quit_preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_delete_browsing_data_on_quit))

        // Delete Browsing Data on Quit Switch
        val deleteOnQuitPref = findPreference<SwitchPreference>(
            getPreferenceKey(R.string.pref_key_delete_browsing_data_on_quit)
        )

        val settings = requireContext().settings()
        deleteOnQuitPref?.apply {
            onPreferenceChangeListener = BooleanSharedPreferenceUpdater {
                setAllCheckboxes(it)
            }
            isChecked = settings.shouldDeleteBrowsingDataOnQuit
        }

        val checkboxUpdater = BooleanSharedPreferenceUpdater {
            if (!settings.shouldDeleteAnyDataOnQuit()) {
                deleteOnQuitPref?.isChecked = false
                settings.shouldDeleteBrowsingDataOnQuit = false
            }
        }

        checkboxes.forEach { (_, pref) ->
            pref.onPreferenceChangeListener = checkboxUpdater
        }
    }

    private fun setAllCheckboxes(newValue: Boolean) {
        checkboxes.forEach { (type, pref) ->
            pref.isChecked = newValue
            pref.context.settings().setDeleteDataOnQuit(type, newValue)
        }
    }
}
