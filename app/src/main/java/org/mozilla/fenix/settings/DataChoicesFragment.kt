/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.MetricServiceType
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar
import kotlin.system.exitProcess

/**
 * Lets the user toggle telemetry on/off.
 */
class DataChoicesFragment : PreferenceFragmentCompat() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = requireContext()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this) { _, key ->
            if (key == getPreferenceKey(R.string.pref_key_telemetry)) {
                if (context.settings().isTelemetryEnabled) {
                    context.components.analytics.metrics.start(MetricServiceType.Data)
                } else {
                    context.components.analytics.metrics.stop(MetricServiceType.Data)
                }
                // Reset experiment identifiers on both opt-in and opt-out; it's likely
                // that in future we will need to pass in the new telemetry client_id
                // to this method when the user opts back in.
                context.components.analytics.experiments.resetTelemetryIdentifiers()
            } else if (key == getPreferenceKey(R.string.pref_key_marketing_telemetry)) {
                if (context.settings().isMarketingTelemetryEnabled) {
                    context.components.analytics.metrics.start(MetricServiceType.Marketing)
                } else {
                    context.components.analytics.metrics.stop(MetricServiceType.Marketing)
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

        requirePreference<SwitchPreference>(R.string.pref_key_telemetry).apply {
            isChecked = context.settings().isTelemetryEnabled

            val appName = context.getString(R.string.app_name)
            summary = context.getString(R.string.preferences_usage_data_description, appName)

            onPreferenceChangeListener = SharedPreferenceUpdater()
        }

        requirePreference<SwitchPreference>(R.string.pref_key_marketing_telemetry).apply {
            isChecked = context.settings().isMarketingTelemetryEnabled
            onPreferenceChangeListener = SharedPreferenceUpdater()
        }

        requirePreference<SwitchPreference>(R.string.pref_key_experimentation).apply {
            isChecked = context.settings().isExperimentationEnabled

            setOnPreferenceChangeListener<Boolean> { preference, enabled ->
                val builder = AlertDialog.Builder(context)
                    .setPositiveButton(
                        R.string.top_sites_rename_dialog_ok
                    ) { dialog, _ ->
                        context.settings().preferences.edit {
                            putBoolean(preference.key, enabled).commit()
                        }
                        context.components.analytics.experiments.globalUserParticipation = enabled
                        dialog.dismiss()
                        exitProcess(0)
                    }
                    .setNegativeButton(
                        R.string.top_sites_rename_dialog_cancel
                    ) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setTitle(R.string.preference_experiments_2)
                    .setMessage(getQuittingAppString())
                    .setCancelable(false)
                val alertDialog: AlertDialog = builder.create()
                alertDialog.show()
                false
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun getQuittingAppString(): String {
        // Fix for #20919. As we are not able to get new strings on Beta and Release,
        // We are using a string that it's already translated and taking some parts of it.
        // To be specific "Firefox Account/Sync server modified. Quitting the application to apply changes…"
        // We are interested on the phrase after the dot, that is generic and we can use for this case.
        val rawString = getString(R.string.toast_override_fxa_sync_server_done)
        return try {
            rawString.split(".")[1]
        } catch (e: Exception) {
            rawString
        }
    }

    companion object {
        private const val OVERRIDE_EXIT_DELAY = 3000L
    }
}
