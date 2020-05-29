/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.support.test.robolectric.testContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.settings.account.AccountAuthErrorPreference
import org.mozilla.fenix.settings.account.AccountPreference
import org.mozilla.fenix.utils.Settings

@RunWith(FenixRobolectricTestRunner::class)
class SettingsAccountViewTest {

    private lateinit var accountView: SettingsAccountView
    private lateinit var fragment: PreferenceFragmentCompat
    private lateinit var accountManager: FxaAccountManager
    private lateinit var settings: Settings
    private lateinit var lifecycle: Lifecycle

    private lateinit var preferenceSignIn: Preference
    private lateinit var preferenceFirefoxAccount: AccountPreference
    private lateinit var preferenceFirefoxAccountAuthError: AccountAuthErrorPreference
    private lateinit var accountPreferenceCategory: PreferenceCategory
    private lateinit var preferenceFxAOverride: Preference
    private lateinit var preferenceSyncOverride: Preference

    @Before
    fun setup() {
        fragment = mockk()
        settings = mockk(relaxed = true)
        accountManager = mockk(relaxed = true)
        val owner = LifecycleOwner { lifecycle }
        lifecycle = LifecycleRegistry(owner)

        preferenceSignIn = mockk(relaxed = true)
        preferenceFirefoxAccount = mockk(relaxed = true)
        preferenceFirefoxAccountAuthError = mockk(relaxed = true)
        accountPreferenceCategory = mockk(relaxed = true)
        preferenceFxAOverride = mockk(relaxed = true)
        preferenceSyncOverride = mockk(relaxed = true)

        mockkStatic("org.mozilla.fenix.ext.ContextKt")

        val context = spyk(testContext)
        every { fragment.requireContext() } returns context
        every { fragment.viewLifecycleOwner } returns owner
        every { fragment.findPreference<Preference>("pref_key_sign_in") } returns preferenceSignIn
        every { fragment.findPreference<AccountPreference>("pref_key_account") } returns preferenceFirefoxAccount
        every { fragment.findPreference<AccountAuthErrorPreference>("pref_key_account_auth_error") } returns preferenceFirefoxAccountAuthError
        every { fragment.findPreference<PreferenceCategory>("pref_key_account_category") } returns accountPreferenceCategory
        every { fragment.findPreference<Preference>("pref_key_override_fxa_server") } returns preferenceFxAOverride
        every { fragment.findPreference<Preference>("pref_key_override_sync_tokenserver") } returns preferenceSyncOverride
        every { context.components.core.client } returns mockk()
        every { context.components.backgroundServices.accountManager } returns accountManager
        every { context.settings() } returns settings
        every { accountManager.accountProfile() } returns null

        accountView = SettingsAccountView(fragment)
    }

    @Test
    fun testRegistersObserver() {
        verify { accountManager.register(any(), any(), autoPause = true) }
    }

    @Test
    fun testSignedOutUi() {
        every { accountManager.authenticatedAccount() } returns null
        accountView.updateAccountUIState()

        verify { preferenceSignIn.isVisible = true }
        verify { preferenceFirefoxAccount.isVisible = false }
        verify { preferenceFirefoxAccountAuthError.isVisible = false }
        verify { accountPreferenceCategory.isVisible = false }

        verify { preferenceSyncOverride.isEnabled = false }
        verify { preferenceSyncOverride.isEnabled = false }
    }

    @Test
    fun testAuthenticatedUi() {
        every { accountManager.authenticatedAccount() } returns mockk()
        every { accountManager.accountNeedsReauth() } returns false
        accountView.updateAccountUIState()

        verify { preferenceSignIn.isVisible = false }
        verify { accountPreferenceCategory.isVisible = true }
        verify { preferenceSignIn.onPreferenceClickListener = null }
        verify { preferenceFirefoxAccountAuthError.isVisible = false }
        verify { preferenceFirefoxAccount.isVisible = true }
        verify { preferenceFirefoxAccount.displayName = null }
        verify { preferenceFirefoxAccount.email = null }

        verify { preferenceSyncOverride.isEnabled = true }
        verify { preferenceSyncOverride.isEnabled = true }
    }

    @Test
    fun testReauthUi() {
        every { accountManager.authenticatedAccount() } returns mockk()
        every { accountManager.accountNeedsReauth() } returns true
        accountView.updateAccountUIState()

        verify { preferenceSignIn.isVisible = false }
        verify { accountPreferenceCategory.isVisible = true }
        verify { preferenceFirefoxAccount.isVisible = false }
        verify { preferenceFirefoxAccountAuthError.isVisible = true }
        verify { preferenceSignIn.onPreferenceClickListener = null }
        verify { preferenceFirefoxAccountAuthError.email = null }
    }
}
