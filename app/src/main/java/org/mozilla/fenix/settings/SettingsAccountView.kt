/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.os.Handler
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.concept.sync.AccountObserver
import mozilla.components.concept.sync.AuthType
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.concept.sync.Profile
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.toRoundedDrawable
import org.mozilla.fenix.settings.account.AccountAuthErrorPreference
import org.mozilla.fenix.settings.account.AccountPreference
import kotlin.system.exitProcess

/**
 * Updates the view for the account controls located at the top of the [SettingsFragment].
 */
class SettingsAccountView(fragment: PreferenceFragmentCompat) {

    private val context = fragment.requireContext()
    private val settings = context.settings()
    private val accountManager = context.components.backgroundServices.accountManager
    private val lifecycleOwner = fragment.viewLifecycleOwner

    private val preferenceSignIn = fragment.findPreference<Preference>(
        context.getPreferenceKey(R.string.pref_key_sign_in)
    )!!
    private val preferenceFirefoxAccount = fragment.findPreference<AccountPreference>(
        context.getPreferenceKey(R.string.pref_key_account)
    )!!
    private val preferenceFirefoxAccountAuthError = fragment.findPreference<AccountAuthErrorPreference>(
        context.getPreferenceKey(R.string.pref_key_account_auth_error)
    )!!
    private val accountPreferenceCategory = fragment.findPreference<PreferenceCategory>(
        context.getPreferenceKey(R.string.pref_key_account_category)
    )!!

    private val preferenceFxAOverride = fragment.findPreference<Preference>(
        context.getPreferenceKey(R.string.pref_key_override_fxa_server)
    )!!
    private val preferenceSyncOverride = fragment.findPreference<Preference>(
        context.getPreferenceKey(R.string.pref_key_override_sync_tokenserver)
    )!!

    private val accountObserver = object : AccountObserver {
        private fun updateAccountUi(profile: Profile? = null) {
            lifecycleOwner.lifecycleScope.launch {
                updateAccountUIState(profile)
            }
        }

        override fun onAuthenticated(account: OAuthAccount, authType: AuthType) = updateAccountUi()
        override fun onLoggedOut() = updateAccountUi()
        override fun onProfileUpdated(profile: Profile) = updateAccountUi(profile)
        override fun onAuthenticationProblems() = updateAccountUi()
    }

    private val syncFxAOverrideUpdater = StringSharedPreferenceUpdater {
        updateFxASyncOverrideMenu(accountManager.authenticatedAccount() != null)
        Toast.makeText(
            context,
            context.getString(R.string.toast_override_fxa_sync_server_done),
            Toast.LENGTH_LONG
        ).show()
        Handler().postDelayed({ exitProcess(0) }, FXA_SYNC_OVERRIDE_EXIT_DELAY)
    }

    init {
        // Observe account changes to keep the UI up-to-date.
        accountManager.register(
            accountObserver,
            owner = lifecycleOwner,
            autoPause = true
        )

        // It's important to update the account UI state in onCreate since that ensures we'll never
        // display an incorrect state in the UI. We take care to not also call it as part of onResume
        // if it was just called here (via the 'creatingFragment' flag).
        // For example, if user is signed-in, and we don't perform this call in onCreate, we'll briefly
        // display a "Sign In" preference, which will then get replaced by the correct account information
        // once this call is ran in onResume shortly after.
        updateAccountUIState()
    }

    /**
     * Updates the UI to reflect current account state.
     * Possible conditions are logged-in without problems, logged-out, and logged-in but needs to re-authenticate.
     */
    fun updateAccountUIState(givenProfile: Profile? = null) {
        val profile = givenProfile ?: accountManager.accountProfile()
        val hasAuthenticatedAccount = accountManager.authenticatedAccount() != null
        updateFxASyncOverrideMenu(hasAuthenticatedAccount)

        if (hasAuthenticatedAccount) {
            preferenceSignIn.isVisible = false
            accountPreferenceCategory.isVisible = true

            if (accountManager.accountNeedsReauth()) {
                // Signed-in, need to re-authenticate.

                preferenceFirefoxAccount.isVisible = false
                preferenceFirefoxAccountAuthError.isVisible = true

                preferenceSignIn.onPreferenceClickListener = null

                preferenceFirefoxAccountAuthError.email = profile?.email
            } else {
                // Signed-in, no problems.

                profile?.avatar?.url?.let { avatarUrl ->
                    lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                        val roundedDrawable =
                            avatarUrl.toRoundedDrawable(context, context.components.core.client)
                        preferenceFirefoxAccount.icon =
                            roundedDrawable ?: getDrawable(context, R.drawable.ic_account)
                    }
                }
                preferenceSignIn.onPreferenceClickListener = null
                preferenceFirefoxAccountAuthError.isVisible = false
                preferenceFirefoxAccount.isVisible = true

                preferenceFirefoxAccount.displayName = profile?.displayName
                preferenceFirefoxAccount.email = profile?.email
            }
        } else {
            // Signed-out.
            preferenceSignIn.isVisible = true
            preferenceFirefoxAccount.isVisible = false
            preferenceFirefoxAccountAuthError.isVisible = false
            accountPreferenceCategory.isVisible = false
        }
    }

    fun setupOverridePreferences() {
        preferenceFxAOverride.onPreferenceChangeListener = syncFxAOverrideUpdater
        preferenceSyncOverride.onPreferenceChangeListener = syncFxAOverrideUpdater
    }

    /**
     * @param hasAuthenticatedAccount Only enable changes to these prefs
     * when the user isn't connected to an account.
     */
    private fun updateFxASyncOverrideMenu(hasAuthenticatedAccount: Boolean) {
        val show = settings.overrideFxAServer.isNotEmpty() ||
            settings.overrideSyncTokenServer.isNotEmpty() ||
            settings.showSecretDebugMenuThisSession

        preferenceFxAOverride.apply {
            isVisible = show
            isEnabled = hasAuthenticatedAccount
            summary = settings.overrideFxAServer.ifEmpty { null }
        }
        preferenceSyncOverride.apply {
            isVisible = show
            isEnabled = hasAuthenticatedAccount
            summary = settings.overrideSyncTokenServer.ifEmpty { null }
        }
    }

    companion object {
        private const val FXA_SYNC_OVERRIDE_EXIT_DELAY = 2000L
    }
}
