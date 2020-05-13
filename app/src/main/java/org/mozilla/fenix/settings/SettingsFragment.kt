/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mozilla.components.concept.sync.AccountObserver
import mozilla.components.concept.sync.AuthType
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.concept.sync.Profile
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.Config
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.application
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.ext.metrics
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.ext.toRoundedDrawable
import org.mozilla.fenix.settings.account.AccountAuthErrorPreference
import org.mozilla.fenix.settings.account.AccountPreference
import kotlin.system.exitProcess

@Suppress("LargeClass", "TooManyFunctions")
class SettingsFragment : PreferenceFragmentCompat() {

    private val accountObserver = object : AccountObserver {
        private fun updateAccountUi(profile: Profile? = null) {
            val context = context ?: return
            lifecycleScope.launch {
                updateAccountUIState(
                    context = context,
                    profile = profile
                        ?: context.components.backgroundServices.accountManager.accountProfile()
                )
            }
        }

        override fun onAuthenticated(account: OAuthAccount, authType: AuthType) = updateAccountUi()
        override fun onLoggedOut() = updateAccountUi()
        override fun onProfileUpdated(profile: Profile) = updateAccountUi(profile)
        override fun onAuthenticationProblems() = updateAccountUi()
    }

    // A flag used to track if we're going through the onCreate->onStart->onResume lifecycle chain.
    // If it's set to `true`, code in `onResume` can assume that `onCreate` executed a moment prior.
    // This flag is set to `false` at the end of `onResume`.
    private var creatingFragment = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Observe account changes to keep the UI up-to-date.
        requireComponents.backgroundServices.accountManager.register(
            accountObserver,
            owner = this,
            autoPause = true
        )

        // It's important to update the account UI state in onCreate since that ensures we'll never
        // display an incorrect state in the UI. We take care to not also call it as part of onResume
        // if it was just called here (via the 'creatingFragment' flag).
        // For example, if user is signed-in, and we don't perform this call in onCreate, we'll briefly
        // display a "Sign In" preference, which will then get replaced by the correct account information
        // once this call is ran in onResume shortly after.
        updateAccountUIState(
            requireContext(),
            requireComponents.backgroundServices.accountManager.accountProfile()
        )

        preferenceManager.sharedPreferences
            .registerOnSharedPreferenceChangeListener(this) { sharedPreferences, key ->
                try {
                    context?.let { context ->
                        context.components.analytics.metrics.track(
                            Event.PreferenceToggled(
                                key,
                                sharedPreferences.getBoolean(key, false),
                                context
                            )
                        )
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

        showToolbar(getString(R.string.settings_title))

        // Account UI state is updated as part of `onCreate`. To not do it twice in a row, we only
        // update it here if we're not going through the `onCreate->onStart->onResume` lifecycle chain.
        update(shouldUpdateAccountUIState = !creatingFragment)

        requireView().findViewById<RecyclerView>(R.id.recycler_view)
            ?.hideInitialScrollBar(viewLifecycleOwner.lifecycleScope)

        // Consider finish of `onResume` to be the point at which we consider this fragment as 'created'.
        creatingFragment = false
    }

    private fun update(shouldUpdateAccountUIState: Boolean) {
        val trackingProtectionPreference =
            findPreference<Preference>(getPreferenceKey(R.string.pref_key_tracking_protection_settings))
        trackingProtectionPreference?.summary = context?.let {
            if (it.settings().shouldUseTrackingProtection) {
                getString(R.string.tracking_protection_on)
            } else {
                getString(R.string.tracking_protection_off)
            }
        }

        val toolbarPreference =
            findPreference<Preference>(getPreferenceKey(R.string.pref_key_toolbar))
        toolbarPreference?.summary = context?.settings()?.toolbarSettingString

        val aboutPreference = findPreference<Preference>(getPreferenceKey(R.string.pref_key_about))
        val appName = getString(R.string.app_name)
        aboutPreference?.title = getString(R.string.preferences_about, appName)

        val deleteBrowsingDataPreference =
            findPreference<Preference>(
                getPreferenceKey(
                    R.string.pref_key_delete_browsing_data_on_quit_preference
                )
            )
        deleteBrowsingDataPreference?.summary = context?.let {
            if (it.settings().shouldDeleteBrowsingDataOnQuit) {
                getString(R.string.delete_browsing_data_quit_on)
            } else {
                getString(R.string.delete_browsing_data_quit_off)
            }
        }

        setupPreferences()

        if (shouldUpdateAccountUIState) {
            updateAccountUIState(
                requireContext(),
                requireComponents.backgroundServices.accountManager.accountProfile()
            )
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
                    SupportUtils.getPrivacyNoticeUrl()
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
        val leakKey = getPreferenceKey(R.string.pref_key_leakcanary)
        val debuggingKey = getPreferenceKey(R.string.pref_key_remote_debugging)

        val preferenceLeakCanary = findPreference<Preference>(leakKey)
        val preferenceRemoteDebugging = findPreference<Preference>(debuggingKey)

        if (!Config.channel.isReleased) {
            preferenceLeakCanary?.setOnPreferenceChangeListener { _, newValue ->
                val isEnabled = newValue == true
                context?.application?.updateLeakCanaryState(isEnabled)
                true
            }
        }

        preferenceRemoteDebugging?.setOnPreferenceChangeListener { preference, newValue ->
            preference.context.settings().preferences.edit()
                .putBoolean(preference.key, newValue as Boolean).apply()
            requireComponents.core.engine.settings.remoteDebuggingEnabled = newValue
            true
        }

        val preferenceFxAOverride =
            findPreference<Preference>(getPreferenceKey(R.string.pref_key_override_fxa_server))
        val preferenceSyncOverride =
            findPreference<Preference>(getPreferenceKey(R.string.pref_key_override_sync_tokenserver))

        val syncFxAOverrideUpdater = object : StringSharedPreferenceUpdater() {
            override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
                return super.onPreferenceChange(preference, newValue).also {
                    updateFxASyncOverrideMenu()
                    Toast.makeText(
                        context,
                        getString(R.string.toast_override_fxa_sync_server_done),
                        Toast.LENGTH_LONG
                    ).show()
                    Handler().postDelayed({
                        exitProcess(0)
                    }, FXA_SYNC_OVERRIDE_EXIT_DELAY)
                }
            }
        }
        preferenceFxAOverride?.onPreferenceChangeListener = syncFxAOverrideUpdater
        preferenceSyncOverride?.onPreferenceChangeListener = syncFxAOverrideUpdater
        findPreference<Preference>(
            getPreferenceKey(R.string.pref_key_debug_settings)
        )?.isVisible = requireContext().settings().showSecretDebugMenuThisSession
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

    /**
     * Updates the UI to reflect current account state.
     * Possible conditions are logged-in without problems, logged-out, and logged-in but needs to re-authenticate.
     */
    private fun updateAccountUIState(context: Context, profile: Profile?) {
        val preferenceSignIn =
            findPreference<Preference>(context.getPreferenceKey(R.string.pref_key_sign_in))
        val preferenceFirefoxAccount =
            findPreference<AccountPreference>(context.getPreferenceKey(R.string.pref_key_account))
        val preferenceFirefoxAccountAuthError =
            findPreference<AccountAuthErrorPreference>(
                context.getPreferenceKey(
                    R.string.pref_key_account_auth_error
                )
            )
        val accountPreferenceCategory =
            findPreference<PreferenceCategory>(context.getPreferenceKey(R.string.pref_key_account_category))

        val accountManager = requireComponents.backgroundServices.accountManager
        val account = accountManager.authenticatedAccount()

        updateFxASyncOverrideMenu()

        // Signed-in, no problems.
        if (account != null && !accountManager.accountNeedsReauth()) {
            preferenceSignIn?.isVisible = false

            profile?.avatar?.url?.let { avatarUrl ->
                lifecycleScope.launch(Main) {
                    val roundedDrawable =
                        avatarUrl.toRoundedDrawable(context, requireComponents.core.client)
                    preferenceFirefoxAccount?.icon =
                        roundedDrawable ?: AppCompatResources.getDrawable(
                            context,
                            R.drawable.ic_account
                        )
                }
            }
            preferenceSignIn?.onPreferenceClickListener = null
            preferenceFirefoxAccountAuthError?.isVisible = false
            preferenceFirefoxAccount?.isVisible = true
            accountPreferenceCategory?.isVisible = true

            preferenceFirefoxAccount?.displayName = profile?.displayName
            preferenceFirefoxAccount?.email = profile?.email

            // Signed-in, need to re-authenticate.
        } else if (account != null && accountManager.accountNeedsReauth()) {
            preferenceFirefoxAccount?.isVisible = false
            preferenceFirefoxAccountAuthError?.isVisible = true
            accountPreferenceCategory?.isVisible = true

            preferenceSignIn?.isVisible = false
            preferenceSignIn?.onPreferenceClickListener = null

            preferenceFirefoxAccountAuthError?.email = profile?.email

            // Signed-out.
        } else {
            preferenceSignIn?.isVisible = true
            preferenceFirefoxAccount?.isVisible = false
            preferenceFirefoxAccountAuthError?.isVisible = false
            accountPreferenceCategory?.isVisible = false
        }
    }

    private fun updateFxASyncOverrideMenu() {
        val preferenceFxAOverride =
            findPreference<Preference>(getPreferenceKey(R.string.pref_key_override_fxa_server))
        val preferenceSyncOverride =
            findPreference<Preference>(getPreferenceKey(R.string.pref_key_override_sync_tokenserver))
        val settings = requireContext().settings()
        val show = settings.overrideFxAServer.isNotEmpty() ||
                settings.overrideSyncTokenServer.isNotEmpty() ||
                settings.showSecretDebugMenuThisSession
        // Only enable changes to these prefs when the user isn't connected to an account.
        val enabled =
            requireComponents.backgroundServices.accountManager.authenticatedAccount() == null
        preferenceFxAOverride?.apply {
            isVisible = show
            isEnabled = enabled
            summary = settings.overrideFxAServer.ifEmpty { null }
        }
        preferenceSyncOverride?.apply {
            isVisible = show
            isEnabled = enabled
            summary = settings.overrideSyncTokenServer.ifEmpty { null }
        }
    }

    companion object {
        private const val SCROLL_INDICATOR_DELAY = 10L
        private const val FXA_SYNC_OVERRIDE_EXIT_DELAY = 2000L
    }
}
