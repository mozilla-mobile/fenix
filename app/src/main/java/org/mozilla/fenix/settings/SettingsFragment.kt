/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceFragmentCompat
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.FenixApplication
import org.mozilla.fenix.R
import org.mozilla.fenix.R.string.pref_key_about
import org.mozilla.fenix.R.string.pref_key_leakcanary
import org.mozilla.fenix.R.string.pref_key_feedback
import org.mozilla.fenix.R.string.pref_key_help
import org.mozilla.fenix.R.string.pref_key_make_default_browser
import org.mozilla.fenix.R.string.pref_key_rate
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.ext.requireComponents
import android.net.Uri
import org.mozilla.fenix.HomeActivity

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.show()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()

        setupPreferences()
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            resources.getString(pref_key_help) -> {
                requireComponents.useCases.tabsUseCases.addTab
                    .invoke(SupportUtils.getSumoURLForTopic(context!!, SupportUtils.SumoTopic.HELP))
                navigateToSettingsArticle()
            }
            resources.getString(pref_key_rate) -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SupportUtils.RATE_APP_URL)))
            }
            resources.getString(pref_key_feedback) -> {
                requireComponents.useCases.tabsUseCases.addTab.invoke(SupportUtils.FEEDBACK_URL)
                navigateToSettingsArticle()
            }
            resources.getString(pref_key_about) -> {
                requireComponents.useCases.tabsUseCases.addTab.invoke(aboutURL, true)
                navigateToSettingsArticle()
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun setupPreferences() {
        val makeDefaultBrowserKey = context?.getPreferenceKey(pref_key_make_default_browser)
        val leakKey = context?.getPreferenceKey(pref_key_leakcanary)

        val preferenceMakeDefaultBrowser = findPreference<Preference>(makeDefaultBrowserKey)
        val preferenceLeakCanary = findPreference<Preference>(leakKey)

        preferenceMakeDefaultBrowser.onPreferenceClickListener =
            getClickListenerForMakeDefaultBrowser()

        preferenceLeakCanary.isVisible = BuildConfig.DEBUG
        if (BuildConfig.DEBUG) {
            preferenceLeakCanary.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { _, newValue ->
                    (context?.applicationContext as FenixApplication).toggleLeakCanary(newValue as Boolean)
                    true
                }
        }
    }

    private val defaultClickListener = OnPreferenceClickListener { preference ->
        Toast.makeText(context, "${preference.title} Clicked", Toast.LENGTH_SHORT).show()
        true
    }

    private fun getClickListenerForMakeDefaultBrowser(): OnPreferenceClickListener {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            OnPreferenceClickListener {
                val intent = Intent(
                    Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS
                )
                startActivity(intent)
                true
            }
        } else {
            defaultClickListener
        }
    }

    private fun navigateToSettingsArticle() {
        val newSession = requireComponents.core.sessionManager.selectedSession?.id
        val directions = SettingsFragmentDirections.actionSettingsFragmentToBrowserFragment(newSession,
            (activity as HomeActivity).browsingModeManager.isPrivate)
        Navigation.findNavController(view!!).navigate(directions)
    }

    companion object {
        const val aboutURL = "about:version"
    }
}
