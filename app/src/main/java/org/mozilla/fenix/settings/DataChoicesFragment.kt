/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import org.mozilla.fenix.Config
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar

/**
 * Lets the user toggle telemetry on/off.
 */
class DataChoicesFragment : PreferenceFragmentCompat() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = requireContext()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this) { sharedPreferences, key ->
            if (key == getPreferenceKey(R.string.pref_key_telemetry)) {
                if (sharedPreferences.getBoolean(key, context.settings().isTelemetryEnabled)) {
                    context.components.analytics.metrics.start()
                } else {
                    context.components.analytics.metrics.stop()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_data_collection))
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.data_choices_preferences, rootKey)

        findPreference<SwitchPreference>(getPreferenceKey(R.string.pref_key_telemetry))?.apply {
            isChecked = context.settings().isTelemetryEnabled

            val appName = context.getString(R.string.app_name)
            summary = context.getString(R.string.preferences_usage_data_description, appName)

            onPreferenceChangeListener = SharedPreferenceUpdater()
        }

        findPreference<SwitchPreference>(getPreferenceKey(R.string.pref_key_experimentation))?.apply {
            isChecked = context.settings().isExperimentationEnabled
            isVisible = Config.channel.isReleaseOrBeta
            onPreferenceChangeListener = SharedPreferenceUpdater()
        }
    }
}
