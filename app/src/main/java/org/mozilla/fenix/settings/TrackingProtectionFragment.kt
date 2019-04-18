/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.ext.requireComponents

class TrackingProtectionFragment : PreferenceFragmentCompat() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).title = getString(R.string.preferences_tracking_protection)
        (activity as AppCompatActivity).supportActionBar?.show()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.tracking_protection_preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        // Tracking Protection Switch
        val trackingProtectionKey =
            context!!.getPreferenceKey(R.string.pref_key_tracking_protection)
        val preferenceTP = findPreference<Preference>(trackingProtectionKey)
        preferenceTP?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                with(requireComponents.core) {
                    val policy =
                        createTrackingProtectionPolicy(newValue as Boolean)
                    engine.settings.trackingProtectionPolicy = policy

                    with(sessionManager) {
                        sessions.forEach { getEngineSession(it)?.enableTrackingProtection(policy) }
                    }
                }
                requireContext().components.useCases.sessionUseCases.reload.invoke()
                true
            }
    }
}
