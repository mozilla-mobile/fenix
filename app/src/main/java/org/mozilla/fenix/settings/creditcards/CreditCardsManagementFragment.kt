/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.creditcards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.lib.state.ext.consumeFrom
import org.mozilla.fenix.R
import org.mozilla.fenix.SecureFragment
import org.mozilla.fenix.components.StoreProvider
import org.mozilla.fenix.databinding.ComponentCreditCardsBinding
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.redirectToReAuth
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.settings.creditcards.controller.DefaultCreditCardsManagementController
import org.mozilla.fenix.settings.creditcards.interactor.CreditCardsManagementInteractor
import org.mozilla.fenix.settings.creditcards.interactor.DefaultCreditCardsManagementInteractor
import org.mozilla.fenix.settings.creditcards.view.CreditCardsManagementView

/**
 * Displays a list of saved credit cards.
 */
class CreditCardsManagementFragment : SecureFragment() {

    private lateinit var creditCardsStore: CreditCardsFragmentStore
    private lateinit var interactor: CreditCardsManagementInteractor
    private lateinit var creditCardsView: CreditCardsManagementView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(CreditCardsManagementView.LAYOUT_ID, container, false)

        creditCardsStore = StoreProvider.get(this) {
            CreditCardsFragmentStore(CreditCardsListState(creditCards = emptyList()))
        }

        interactor = DefaultCreditCardsManagementInteractor(
            controller = DefaultCreditCardsManagementController(
                navController = findNavController()
            ),
            requireContext().components.analytics.metrics
        )
        val binding = ComponentCreditCardsBinding.bind(view)

        creditCardsView = CreditCardsManagementView(binding, interactor)

        loadCreditCards()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        consumeFrom(creditCardsStore) { state ->
            if (!state.isLoading && state.creditCards.isEmpty()) {
                findNavController().popBackStack()
                return@consumeFrom
            }

            creditCardsView.update(state)
        }
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.credit_cards_saved_cards))
    }

    /**
     * When the fragment is paused, navigate back to the settings page to reauthenticate.
     */
    override fun onPause() {
        // Don't redirect if the user is navigating to the credit card editor fragment.
        redirectToReAuth(
            listOf(R.id.creditCardEditorFragment),
            findNavController().currentDestination?.id,
            R.id.creditCardsManagementFragment
        )

        super.onPause()
    }

    /**
     * Fetches all the credit cards from the autofill storage and updates the
     * [CreditCardsFragmentStore] with the list of credit cards.
     */
    private fun loadCreditCards() {
        if (requireContext().components.core.autofillStorage.hasStorage()) {
            lifecycleScope.launch(Dispatchers.IO) {
                val creditCards =
                    requireContext().components.core.autofillStorage.getAllCreditCards()

                lifecycleScope.launch(Dispatchers.Main) {
                    creditCardsStore.dispatch(CreditCardsAction.UpdateCreditCards(creditCards))
                }
            }
        }
    }
}
