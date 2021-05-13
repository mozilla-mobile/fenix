/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.creditcards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import mozilla.components.concept.storage.CreditCard
import mozilla.components.lib.state.ext.consumeFrom
import mozilla.components.service.fxa.SyncEngine
import org.mozilla.fenix.R
import org.mozilla.fenix.components.StoreProvider
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.ext.navigateBlockingForAsyncNavGraph
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.settings.SyncPreferenceView
import org.mozilla.fenix.settings.requirePreference

/**
 * "Credit cards" settings fragment displays a list of settings related to autofilling, adding and
 * syncing credit cards.
 */
class CreditCardsSettingFragment : PreferenceFragmentCompat() {

    private lateinit var creditCardsStore: CreditCardsFragmentStore
    private var isCreditCardsListLoaded: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        creditCardsStore = StoreProvider.get(this) {
            CreditCardsFragmentStore(CreditCardsListState(creditCards = emptyList()))
        }
        loadCreditCards()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.credit_cards_preferences, rootKey)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        loadCreditCards()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        consumeFrom(creditCardsStore) { state ->
            updateCardManagementPreferencesVisibility(state.creditCards)
        }
    }

    override fun onPause() {
        super.onPause()
        isCreditCardsListLoaded = false
    }

    override fun onResume() {
        super.onResume()

        showToolbar(getString(R.string.preferences_credit_cards))

        SyncPreferenceView(
            syncPreference = requirePreference(R.string.pref_key_credit_cards_sync_cards_across_devices),
            lifecycleOwner = viewLifecycleOwner,
            accountManager = requireComponents.backgroundServices.accountManager,
            syncEngine = SyncEngine.CreditCards,
            loggedOffTitle = requireContext()
                .getString(R.string.preferences_credit_cards_sync_cards_across_devices),
            loggedInTitle = requireContext()
                .getString(R.string.preferences_credit_cards_sync_cards),
            onSignInToSyncClicked = {
                val directions =
                    CreditCardsSettingFragmentDirections.actionCreditCardsSettingFragmentToTurnOnSyncFragment()
                findNavController().navigateBlockingForAsyncNavGraph(directions)
            },
            onReconnectClicked = {
                val directions =
                    CreditCardsSettingFragmentDirections.actionGlobalAccountProblemFragment()
                findNavController().navigateBlockingForAsyncNavGraph(directions)
            }
        )
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            getPreferenceKey(R.string.pref_key_credit_cards_add_credit_card) -> {
                val directions =
                    CreditCardsSettingFragmentDirections
                        .actionCreditCardsSettingFragmentToCreditCardEditorFragment()
                findNavController().navigateBlockingForAsyncNavGraph(directions)
            }
            getPreferenceKey(R.string.pref_key_credit_cards_manage_saved_cards) -> {
                val directions =
                    CreditCardsSettingFragmentDirections
                        .actionCreditCardsSettingFragmentToCreditCardsManagementFragment()
                findNavController().navigateBlockingForAsyncNavGraph(directions)
            }
        }

        return super.onPreferenceTreeClick(preference)
    }

    /**
     * Updates preferences visibility depending on credit cards being already saved or not.
     */
    @VisibleForTesting
    internal fun updateCardManagementPreferencesVisibility(creditCardsList: List<CreditCard>) {
        val hasCreditCards = creditCardsList.isNotEmpty()

        val manageSavedCardsPreference =
            requirePreference<Preference>(R.string.pref_key_credit_cards_manage_saved_cards)
        val addCreditCardsPreference =
            requirePreference<Preference>(R.string.pref_key_credit_cards_add_credit_card)

        manageSavedCardsPreference.isVisible = hasCreditCards
        addCreditCardsPreference.isVisible = !hasCreditCards
    }

    /**
     * Fetches all the credit cards from autofillStorage and updates the [CreditCardsListState]
     * with the list of credit cards.
     */
    private fun loadCreditCards() {
        if (isCreditCardsListLoaded) {
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val creditCards = requireComponents.core.autofillStorage.getAllCreditCards()
            lifecycleScope.launch(Dispatchers.Main) {
                creditCardsStore.dispatch(CreditCardsAction.UpdateCreditCards(creditCards))
            }
        }

        isCreditCardsListLoaded = true
    }
}
