/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.creditcards

import android.app.KeyguardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.Settings.ACTION_SECURITY_SETTINGS
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mozilla.components.service.fxa.SyncEngine
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.ext.navigateBlockingForAsyncNavGraph
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.runIfFragmentIsAttached
import org.mozilla.fenix.ext.secure
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.settings.SyncPreferenceView
import org.mozilla.fenix.settings.biometric.BiometricPromptHelper
import org.mozilla.fenix.settings.requirePreference

/**
 * Authentication for saved credit cards using [BiometricPromptFeature] or [KeyguardManager].
 * Similar to [SavedLoginsAuthFragment]
 */
class CreditCardsAuthFragment : BiometricPromptHelper() {

    /**
     * Used for toggling preferences on/off during authentication
     */
    private val creditCardPreferences = listOf(
        R.string.pref_key_credit_cards_save_and_autofill_cards,
        R.string.pref_key_credit_cards_sync_cards_across_devices,
        R.string.pref_key_credit_cards_add_credit_card,
        R.string.pref_key_credit_cards_manage_saved_cards
    )

    override fun unlockMessage() = getString(R.string.credit_cards_biometric_prompt_message)

    override fun navigateOnSuccess() = navigateToSavedCreditCards()

    /**
     * Navigates to the [CreditCardsManagementFragment] with a slight delay.
     */
    private fun navigateToSavedCreditCards() {
        runIfFragmentIsAttached {
            viewLifecycleOwner.lifecycleScope.launch(Main) {
                // Workaround for likely biometric library bug
                // https://github.com/mozilla-mobile/fenix/issues/8438
                delay(SHORT_DELAY_MS)
                navigateToCreditCardManagementFragment()
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.credit_cards_preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setBiometricPrompt(view, creditCardPreferences)
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_credit_cards))

        SyncPreferenceView(
            syncPreference = requirePreference(
                R.string.pref_key_credit_cards_sync_cards_across_devices
            ),
            lifecycleOwner = viewLifecycleOwner,
            accountManager = requireComponents.backgroundServices.accountManager,
            syncEngine = SyncEngine.CreditCards,
            loggedOffTitle = requireContext()
                .getString(R.string.preferences_credit_cards_sync_cards_across_devices),
            loggedInTitle = requireContext()
                .getString(R.string.preferences_credit_cards_sync_cards),
            onSignInToSyncClicked = { navigateToSyncFragment() },
            onReconnectClicked = {
                val directions =
                    CreditCardsSettingFragmentDirections.actionGlobalAccountProblemFragment()
                findNavController().navigateBlockingForAsyncNavGraph(directions)
            }
        )

        togglePrefsEnabledWhileAuthenticating(creditCardPreferences, true)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            getPreferenceKey(R.string.pref_key_credit_cards_add_credit_card) -> {
                navigateToCreditCardEditFragment()
            }
            getPreferenceKey(R.string.pref_key_credit_cards_manage_saved_cards) -> {
                verifyCredentialsOrShowSetupWarning(preference.context, creditCardPreferences)
            }
        }

        return super.onPreferenceTreeClick(preference)
    }

    /**
     * Show a warning to set up a pin/password when the device is not secured. This is only used
     * when BiometricPrompt is unavailable on the device.
     */
    override fun showPinDialogWarning(context: Context) {
        AlertDialog.Builder(context).apply {
            setTitle(getString(R.string.credit_cards_warning_dialog_title))
            setMessage(getString(R.string.credit_cards_warning_dialog_message))

            setNegativeButton(getString(R.string.credit_cards_warning_dialog_later))
                { _: DialogInterface, _ ->
                    navigateToCreditCardManagementFragment()
                }

            setPositiveButton(getString(R.string.credit_cards_warning_dialog_set_up_now))
                { it: DialogInterface, _ ->
                    it.dismiss()
                    val intent = Intent(ACTION_SECURITY_SETTINGS)
                    startActivity(intent)
                }

            create()
        }.show().secure(activity)

        // do we want to count these security warnings as well? what are we using this metric for?
        // context.settings().incrementShowLoginsSecureWarningCount()
    }


    /**
     * Create a prompt to confirm the device's pin/password and start activity based on the result.
     * This is only used when BiometricPrompt is unavailable on the device.
     *
     * @param manager The device [KeyguardManager]
     */
    @Suppress("Deprecation")
    override fun showPinVerification(manager: KeyguardManager) {
        val intent = manager.createConfirmDeviceCredentialIntent(
            getString(R.string.credit_cards_biometric_prompt_message_pin),
            getString(R.string.credit_cards_biometric_prompt_message)
        )
        startActivityForResult(intent, PIN_REQUEST)
    }

    /**
     * Called when authentication succeeds.
     */
    private fun navigateToCreditCardManagementFragment() {
        val directions =
            CreditCardsAuthFragmentDirections
                .actionCreditCardAuthFragmentToCreditCardManagementFragment()
        findNavController().navigateBlockingForAsyncNavGraph(directions)
    }

    private fun navigateToSyncFragment() {
        val directions =
            CreditCardsAuthFragmentDirections
                .actionCreditCardAuthFragmentToTurnOnSyncFragment()
        findNavController().navigateBlockingForAsyncNavGraph(directions)
    }

    private fun navigateToCreditCardEditFragment() {
        val directions =
            CreditCardsSettingFragmentDirections
                .actionCreditCardsAuthFragmentToCreditCardEditorFragment()
        findNavController().navigateBlockingForAsyncNavGraph(directions)
    }

    companion object {
        const val SHORT_DELAY_MS = 100L
        const val PIN_REQUEST = 303
    }
}
