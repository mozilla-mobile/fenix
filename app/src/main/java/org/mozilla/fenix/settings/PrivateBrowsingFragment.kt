/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.mozilla.fenix.R
import org.mozilla.fenix.components.PrivateShortcutCreateManager
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.ext.metrics
import org.mozilla.fenix.ext.showToolbar

/**
 * Lets the user customize Private browsing options.
 */
class PrivateBrowsingFragment : PreferenceFragmentCompat() {
    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_private_browsing_options))
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.private_browsing_preferences, rootKey)

        findPreference<Preference>(getPreferenceKey(R.string.pref_key_add_private_browsing_shortcut))?.apply {
            setOnPreferenceClickListener {
                requireContext().metrics.track(Event.PrivateBrowsingCreateShortcut)
                PrivateShortcutCreateManager.createPrivateShortcut(requireContext())
                true
            }
        }
    }
}
