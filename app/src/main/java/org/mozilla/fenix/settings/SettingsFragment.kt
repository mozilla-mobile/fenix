/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mozilla.components.concept.sync.AccountObserver
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.concept.sync.Profile
import mozilla.components.service.fxa.FxaUnauthorizedException
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.Config
import org.mozilla.fenix.FenixApplication
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.R.string.pref_key_about
import org.mozilla.fenix.R.string.pref_key_accessibility
import org.mozilla.fenix.R.string.pref_key_account
import org.mozilla.fenix.R.string.pref_key_account_category
import org.mozilla.fenix.R.string.pref_key_data_choices
import org.mozilla.fenix.R.string.pref_key_feedback
import org.mozilla.fenix.R.string.pref_key_help
import org.mozilla.fenix.R.string.pref_key_language
import org.mozilla.fenix.R.string.pref_key_leakcanary
import org.mozilla.fenix.R.string.pref_key_make_default_browser
import org.mozilla.fenix.R.string.pref_key_rate
import org.mozilla.fenix.R.string.pref_key_remote_debugging
import org.mozilla.fenix.R.string.pref_key_search_engine_settings
import org.mozilla.fenix.R.string.pref_key_sign_in
import org.mozilla.fenix.R.string.pref_key_site_permissions
import org.mozilla.fenix.R.string.pref_key_theme
import org.mozilla.fenix.R.string.pref_key_tracking_protection_settings
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.utils.ItsNotBrokenSnack
import kotlin.coroutines.CoroutineContext

@SuppressWarnings("TooManyFunctions")
class SettingsFragment : PreferenceFragmentCompat(), CoroutineScope, AccountObserver {
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
        updateSignInVisibility()

        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
            try {
                context?.let {
                    it.components.analytics.metrics.track(Event.PreferenceToggled
                    (key, sharedPreferences.getBoolean(key, false), it))
                }
            } catch (e: IllegalArgumentException) {
                // The event is not tracked
            } catch (e: ClassCastException) {
                // The setting is not a boolean, not tracked
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()

        (activity as AppCompatActivity).title = getString(R.string.settings_title)
        (activity as AppCompatActivity).supportActionBar?.show()
        val defaultBrowserPreference =
            findPreference<DefaultBrowserPreference>(getString(R.string.pref_key_make_default_browser))
        defaultBrowserPreference?.updateSwitch()

        val searchEnginePreference =
            findPreference<Preference>(getString(R.string.pref_key_search_engine_settings))
        searchEnginePreference?.summary = context?.let {
            requireComponents.search.searchEngineManager.getDefaultSearchEngine(it).name
        }

        val trackingProtectionPreference =
            findPreference<Preference>(getString(R.string.pref_key_tracking_protection_settings))
        trackingProtectionPreference?.summary = context?.let {
            if (org.mozilla.fenix.utils.Settings.getInstance(it).shouldUseTrackingProtection) {
                getString(R.string.tracking_protection_on)
            } else {
                getString(R.string.tracking_protection_off)
            }
        }

        val themesPreference =
            findPreference<Preference>(getString(R.string.pref_key_theme))
        themesPreference?.summary = context?.let {
            org.mozilla.fenix.utils.Settings.getInstance(it).themeSettingString
        }

        val aboutPreference = findPreference<Preference>(getString(R.string.pref_key_about))
        val appName = getString(R.string.app_name)
        aboutPreference?.title = getString(R.string.preferences_about, appName)

        setupPreferences()
        setupAccountUI()
    }

    @Suppress("ComplexMethod")
    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            resources.getString(pref_key_search_engine_settings) -> {
                navigateToSearchEngineSettings()
            }
            resources.getString(pref_key_tracking_protection_settings) -> {
                navigateToTrackingProtectionSettings()
            }
            resources.getString(pref_key_site_permissions) -> {
                navigateToSitePermissions()
            }
            resources.getString(pref_key_accessibility) -> {
                navigateToAccessibility()
            }
            resources.getString(pref_key_language) -> {
                // TODO #220
                ItsNotBrokenSnack(context!!).showSnackbar(issueNumber = "220")
            }
            resources.getString(pref_key_data_choices) -> {
                navigateToDataChoices()
            }
            resources.getString(pref_key_help) -> {
                (activity as HomeActivity).openToBrowserAndLoad(
                    searchTermOrURL = SupportUtils.getSumoURLForTopic(context!!, SupportUtils.SumoTopic.HELP),
                    newTab = true,
                    from = BrowserDirection.FromSettings
                )
            }
            resources.getString(pref_key_rate) -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SupportUtils.RATE_APP_URL)))
            }
            resources.getString(pref_key_feedback) -> {
                (activity as HomeActivity).openToBrowserAndLoad(
                    searchTermOrURL = SupportUtils.FEEDBACK_URL,
                    newTab = true,
                    from = BrowserDirection.FromSettings
                )
            }
            resources.getString(pref_key_about) -> {
                navigateToAbout()
            }
            resources.getString(pref_key_account) -> {
                navigateToAccountSettings()
            }
            resources.getString(pref_key_theme) -> {
                navigateToThemeSettings()
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun setupAccountUI() {
        val accountManager = requireComponents.backgroundServices.accountManager
        // Observe account changes to keep the UI up-to-date.
        accountManager.register(this, owner = this)

        updateAuthState(accountManager.authenticatedAccount())
        accountManager.accountProfile()?.let { updateAccountProfile(it) }
    }

    private fun getClickListenerForSignIn(): OnPreferenceClickListener {
        return OnPreferenceClickListener {
            val directions = SettingsFragmentDirections.actionSettingsFragmentToTurnOnSyncFragment()
            Navigation.findNavController(view!!).navigate(directions)
            true
        }
    }

    private fun setupPreferences() {
        val makeDefaultBrowserKey = context!!.getPreferenceKey(pref_key_make_default_browser)
        val leakKey = context!!.getPreferenceKey(pref_key_leakcanary)
        val debuggingKey = context!!.getPreferenceKey(pref_key_remote_debugging)

        val preferenceMakeDefaultBrowser = findPreference<Preference>(makeDefaultBrowserKey)
        val preferenceLeakCanary = findPreference<Preference>(leakKey)
        val preferenceRemoteDebugging = findPreference<Preference>(debuggingKey)

        preferenceMakeDefaultBrowser?.onPreferenceClickListener =
            getClickListenerForMakeDefaultBrowser()

        if (!Config.channel.isReleased) {
            preferenceLeakCanary?.setOnPreferenceChangeListener { _, newValue ->
                (context?.applicationContext as FenixApplication).toggleLeakCanary(newValue as Boolean)
                true
            }
        }

        preferenceRemoteDebugging?.setOnPreferenceChangeListener { preference, newValue ->
            org.mozilla.fenix.utils.Settings.getInstance(preference.context).preferences.edit()
                .putBoolean(preference.key, newValue as Boolean).apply()
            requireComponents.core.engine.settings.remoteDebuggingEnabled = newValue
            true
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

    private fun navigateToSearchEngineSettings() {
        val directions = SettingsFragmentDirections.actionSettingsFragmentToSearchEngineFragment()
        Navigation.findNavController(view!!).navigate(directions)
    }

    private fun navigateToTrackingProtectionSettings() {
        val directions = SettingsFragmentDirections.actionSettingsFragmentToTrackingProtectionFragment()
        Navigation.findNavController(view!!).navigate(directions)
    }

    private fun navigateToThemeSettings() {
        val directions = SettingsFragmentDirections.actionSettingsFragmentToThemeFragment()
        Navigation.findNavController(view!!).navigate(directions)
    }

    private fun navigateToSitePermissions() {
        val directions =
            SettingsFragmentDirections.actionSettingsFragmentToSitePermissionsFragment()
        Navigation.findNavController(view!!).navigate(directions)
    }

    private fun navigateToAccessibility() {
        val directions = SettingsFragmentDirections.actionSettingsFragmentToAccessibilityFragment()
        Navigation.findNavController(view!!).navigate(directions)
    }

    private fun navigateToDataChoices() {
        val directions = SettingsFragmentDirections.actionSettingsFragmentToDataChoicesFragment()
        Navigation.findNavController(view!!).navigate(directions)
    }

    private fun navigateToAbout() {
        val directions = SettingsFragmentDirections.actionSettingsFragmentToAboutFragment()
        Navigation.findNavController(view!!).navigate(directions)
    }

    private fun navigateToAccountSettings() {
        val directions =
            SettingsFragmentDirections.actionSettingsFragmentToAccountSettingsFragment()
        Navigation.findNavController(view!!).navigate(directions)
    }

    override fun onAuthenticated(account: OAuthAccount) {
        updateAuthState(account)
    }

    override fun onError(error: Exception) {
        // TODO we could display some error states in this UI.
        when (error) {
            is FxaUnauthorizedException -> {
            }
        }
    }

    override fun onLoggedOut() {
        updateAuthState()
        updateSignInVisibility()
    }

    override fun onProfileUpdated(profile: Profile) {
        updateAccountProfile(profile)
    }

    // --- Account UI helpers ---
    private fun updateAuthState(account: OAuthAccount? = null) {
        // Cache the user's auth state to improve performance of sign in visibility
        org.mozilla.fenix.utils.Settings.getInstance(context!!).setHasCachedAccount(account != null)
    }

    private fun updateSignInVisibility() {
        val hasCachedAccount = org.mozilla.fenix.utils.Settings.getInstance(context!!).hasCachedAccount
        val preferenceSignIn =
            findPreference<Preference>(context!!.getPreferenceKey(pref_key_sign_in))
        val preferenceFirefoxAccount =
            findPreference<Preference>(context!!.getPreferenceKey(pref_key_account))
        val accountPreferenceCategory =
            findPreference<PreferenceCategory>(context!!.getPreferenceKey(pref_key_account_category))

        if (hasCachedAccount) {
            preferenceSignIn?.isVisible = false
            preferenceSignIn?.onPreferenceClickListener = null
            preferenceFirefoxAccount?.isVisible = true
            accountPreferenceCategory?.isVisible = true
        } else {
            preferenceSignIn?.isVisible = true
            preferenceSignIn?.onPreferenceClickListener = getClickListenerForSignIn()
            preferenceFirefoxAccount?.isVisible = false
            accountPreferenceCategory?.isVisible = false
        }
    }

    private fun updateAccountProfile(profile: Profile) {
        launch {
            val preferenceFirefoxAccount =
                findPreference<Preference>(context!!.getPreferenceKey(pref_key_account))
            preferenceFirefoxAccount?.title = profile.displayName.orEmpty()
            preferenceFirefoxAccount?.summary = profile.email.orEmpty()
        }
    }
}
