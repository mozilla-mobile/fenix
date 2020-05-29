/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.Config
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.application
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.ext.metrics
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar

class SettingsFragment : PreferenceFragmentCompat() {

    private var accountView: SettingsAccountView? = null

    // A flag used to track if we're going through the onCreate->onStart->onResume lifecycle chain.
    // If it's set to `true`, code in `onResume` can assume that `onCreate` executed a moment prior.
    // This flag is set to `false` at the end of `onResume`.
    private var creatingFragment = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        accountView = SettingsAccountView(this)
        addMetricsPreferenceListener()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()

        showToolbar(getString(R.string.settings_title))

        // Account UI state is updated as part of `onCreate`. To not do it twice in a row, we only
        // update it here if we're not going through the `onCreate->onStart->onResume` lifecycle chain.
        update(shouldUpdateAccountUIState = !creatingFragment)

        requireView().findViewById<RecyclerView>(R.id.recycler_view)
            ?.hideInitialScrollBar(viewLifecycleOwner.lifecycleScope)

        // Consider finish of `onResume` to be the point at which we consider this fragment as 'created'.
        creatingFragment = false
    }

    override fun onDestroyView() {
        accountView = null
        super.onDestroyView()
    }

    private fun update(shouldUpdateAccountUIState: Boolean) {
        val context = context ?: return
        val settings = context.settings()

        val trackingProtectionPreference =
            findPreference<Preference>(getPreferenceKey(R.string.pref_key_tracking_protection_settings))!!
        trackingProtectionPreference.setSummary(if (settings.shouldUseTrackingProtection) {
            R.string.tracking_protection_on
        } else {
            R.string.tracking_protection_off
        })

        val toolbarPreference =
            findPreference<Preference>(getPreferenceKey(R.string.pref_key_toolbar))!!
        toolbarPreference.summary = settings.toolbarSettingString

        val aboutPreference = findPreference<Preference>(getPreferenceKey(R.string.pref_key_about))!!
        val appName = getString(R.string.app_name)
        aboutPreference.title = getString(R.string.preferences_about, appName)

        val deleteBrowsingDataPreference = findPreference<Preference>(
            getPreferenceKey(R.string.pref_key_delete_browsing_data_on_quit_preference)
        )!!
        deleteBrowsingDataPreference.setSummary(if (settings.shouldDeleteBrowsingDataOnQuit) {
            R.string.delete_browsing_data_quit_on
        } else {
            R.string.delete_browsing_data_quit_off
        })

        setupPreferences()

        if (shouldUpdateAccountUIState) {
            accountView?.updateAccountUIState()
        }
    }

    @Suppress("ComplexMethod", "LongMethod")
    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        // Hide the scrollbar so the animation looks smoother
        val recyclerView = requireView().findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.isVerticalScrollBarEnabled = false

        val directions: NavDirections? = when (preference.key) {
            resources.getString(R.string.pref_key_sign_in) -> {
                SettingsFragmentDirections.actionSettingsFragmentToTurnOnSyncFragment()
            }
            resources.getString(R.string.pref_key_search_settings) -> {
                SettingsFragmentDirections.actionSettingsFragmentToSearchEngineFragment()
            }
            resources.getString(R.string.pref_key_tracking_protection_settings) -> {
                requireContext().metrics.track(Event.TrackingProtectionSettings)
                SettingsFragmentDirections.actionSettingsFragmentToTrackingProtectionFragment()
            }
            resources.getString(R.string.pref_key_site_permissions) -> {
                SettingsFragmentDirections.actionSettingsFragmentToSitePermissionsFragment()
            }
            resources.getString(R.string.pref_key_private_browsing) -> {
                SettingsFragmentDirections.actionSettingsFragmentToPrivateBrowsingFragment()
            }
            resources.getString(R.string.pref_key_accessibility) -> {
                SettingsFragmentDirections.actionSettingsFragmentToAccessibilityFragment()
            }
            resources.getString(R.string.pref_key_language) -> {
                SettingsFragmentDirections.actionSettingsFragmentToLocaleSettingsFragment()
            }
            resources.getString(R.string.pref_key_addons) -> {
                requireContext().metrics.track(Event.AddonsOpenInSettings)
                SettingsFragmentDirections.actionSettingsFragmentToAddonsFragment()
            }
            resources.getString(R.string.pref_key_make_default_browser) -> {
                SettingsFragmentDirections.actionSettingsFragmentToDefaultBrowserSettingsFragment()
            }
            resources.getString(R.string.pref_key_data_choices) -> {
                SettingsFragmentDirections.actionSettingsFragmentToDataChoicesFragment()
            }
            resources.getString(R.string.pref_key_help) -> {
                (activity as HomeActivity).openToBrowserAndLoad(
                    searchTermOrURL = SupportUtils.getSumoURLForTopic(
                        requireContext(),
                        SupportUtils.SumoTopic.HELP
                    ),
                    newTab = true,
                    from = BrowserDirection.FromSettings
                )
                null
            }
            resources.getString(R.string.pref_key_rate) -> {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SupportUtils.RATE_APP_URL)))
                } catch (e: ActivityNotFoundException) {
                    // Device without the play store installed.
                    // Opening the play store website.
                    (activity as HomeActivity).openToBrowserAndLoad(
                        searchTermOrURL = SupportUtils.FENIX_PLAY_STORE_URL,
                        newTab = true,
                        from = BrowserDirection.FromSettings
                    )
                }
                null
            }
            resources.getString(R.string.pref_key_passwords) -> {
                SettingsFragmentDirections.actionSettingsFragmentToSavedLoginsAuthFragment()
            }
            resources.getString(R.string.pref_key_about) -> {
                SettingsFragmentDirections.actionSettingsFragmentToAboutFragment()
            }
            resources.getString(R.string.pref_key_account) -> {
                SettingsFragmentDirections.actionSettingsFragmentToAccountSettingsFragment()
            }
            resources.getString(R.string.pref_key_account_auth_error) -> {
                SettingsFragmentDirections.actionSettingsFragmentToAccountProblemFragment()
            }
            resources.getString(R.string.pref_key_delete_browsing_data) -> {
                SettingsFragmentDirections.actionSettingsFragmentToDeleteBrowsingDataFragment()
            }
            resources.getString(R.string.pref_key_delete_browsing_data_on_quit_preference) -> {
                SettingsFragmentDirections.actionSettingsFragmentToDeleteBrowsingDataOnQuitFragment()
            }
            resources.getString(R.string.pref_key_customize) -> {
                SettingsFragmentDirections.actionSettingsFragmentToCustomizationFragment()
            }
            resources.getString(R.string.pref_key_privacy_link) -> {
                val intent = SupportUtils.createCustomTabIntent(
                    requireContext(),
                    SupportUtils.getMozillaPageUrl(SupportUtils.MozillaPage.PRIVATE_NOTICE)
                )
                startActivity(intent)
                null
            }
            resources.getString(R.string.pref_key_your_rights) -> {
                val context = requireContext()
                val intent = SupportUtils.createCustomTabIntent(
                    context,
                    SupportUtils.getSumoURLForTopic(context, SupportUtils.SumoTopic.YOUR_RIGHTS)
                )
                startActivity(intent)
                null
            }
            resources.getString(R.string.pref_key_debug_settings) -> {
                SettingsFragmentDirections.actionSettingsFragmentToSecretSettingsFragment()
            }
            else -> null
        }
        directions?.let { navigateFromSettings(directions) }
        return super.onPreferenceTreeClick(preference)
    }

    private fun setupPreferences() {
        val preferenceLeakCanary = findPreference<Preference>(
            getPreferenceKey(R.string.pref_key_leakcanary)
        )!!
        val preferenceRemoteDebugging = findPreference<Preference>(
            getPreferenceKey(R.string.pref_key_remote_debugging)
        )!!

        if (!Config.channel.isReleased) {
            preferenceLeakCanary.setOnPreferenceChangeListener<Boolean> { preference, isEnabled ->
                preference.context.application.updateLeakCanaryState(isEnabled)
                true
            }
        }

        preferenceRemoteDebugging.onPreferenceChangeListener = BooleanSharedPreferenceUpdater {
            requireComponents.core.engine.settings.remoteDebuggingEnabled = it
        }

        accountView?.setupOverridePreferences()
        findPreference<Preference>(
            getPreferenceKey(R.string.pref_key_debug_settings)
        )!!.isVisible = requireContext().settings().showSecretDebugMenuThisSession
    }

    private fun navigateFromSettings(directions: NavDirections) {
        view?.findNavController()?.let { navController ->
            if (navController.currentDestination?.id == R.id.settingsFragment) {
                navController.navigate(directions)
            }
        }
    }

    // Extension function for hiding the scroll bar on initial loading. We must do this so the
    // animation to the next screen doesn't animate the initial scroll bar (it ignores
    // isVerticalScrollBarEnabled being set to false).
    private fun RecyclerView.hideInitialScrollBar(scope: CoroutineScope) {
        scope.launch {
            val originalSize = scrollBarSize
            scrollBarSize = 0
            delay(SCROLL_INDICATOR_DELAY)
            scrollBarSize = originalSize
        }
    }

    companion object {
        private const val SCROLL_INDICATOR_DELAY = 10L
    }
}
