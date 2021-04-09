/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.creditcards

import android.os.Bundle
import android.view.WindowManager
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import mozilla.components.service.fxa.SyncEngine
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.settings.SyncPreferenceView
import org.mozilla.fenix.settings.requirePreference

/**
 * "Credit cards" settings fragment displays a list of settings related to autofilling, adding and
 * syncing credit cards.
 */
class CreditCardsSettingFragment : PreferenceFragmentCompat() {

    // bool flag to decide whether to allow screen shots or not
    private var isNextDestinationCreditCardEditor: Boolean = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.credit_cards_preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()

        isNextDestinationCreditCardEditor = false
        // Prevents screenshot and screen capture on this screen
        activity?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        showToolbar(getString(R.string.preferences_credit_cards))

        SyncPreferenceView(
            syncPreference = requirePreference(R.string.pref_key_credit_cards_sync_cards_across_devices),
            lifecycleOwner = viewLifecycleOwner,
            accountManager = requireComponents.backgroundServices.accountManager,
            syncEngine = SyncEngine.Passwords,
            onSignInToSyncClicked = {
                val directions =
                    CreditCardsSettingFragmentDirections.actionCreditCardsSettingFragmentToTurnOnSyncFragment()
                findNavController().navigate(directions)
            },
            onSyncStatusClicked = {
                val directions =
                    CreditCardsSettingFragmentDirections.actionGlobalAccountSettingsFragment()
                findNavController().navigate(directions)
            },
            onReconnectClicked = {
                val directions =
                    CreditCardsSettingFragmentDirections.actionGlobalAccountProblemFragment()
                findNavController().navigate(directions)
            }
        )
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            getPreferenceKey(R.string.pref_key_credit_cards_add_credit_card) -> {
                isNextDestinationCreditCardEditor = true
                val directions =
                    CreditCardsSettingFragmentDirections.actionCreditCardsSettingFragmentToCreditCardEditorFragment()
                findNavController().navigate(directions)
            }
        }

        return super.onPreferenceTreeClick(preference)
    }

    override fun onStop() {
        removeFlagSecure()
        super.onStop()
    }

    /**
     * Checks var isNextDestinationCreditCardEditor to decide whether to remove or keep
     * WindowManager.LayoutParams.FLAG_SECURE
     */
    private fun removeFlagSecure() {
        if (!isNextDestinationCreditCardEditor) {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
