/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.creditcards.view

import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.ComponentCreditCardsBinding
import org.mozilla.fenix.settings.creditcards.CreditCardsListState
import org.mozilla.fenix.settings.creditcards.interactor.CreditCardsManagementInteractor

/**
 * Shows a list of credit cards.
 */
class CreditCardsManagementView(
    val binding: ComponentCreditCardsBinding,
    val interactor: CreditCardsManagementInteractor
) {

    private val creditCardsAdapter = CreditCardsAdapter(interactor)

    init {
        LayoutInflater.from(binding.root.context).inflate(LAYOUT_ID, binding.root, true)

        binding.creditCardsList.apply {
            adapter = creditCardsAdapter
            layoutManager = LinearLayoutManager(binding.root.context)
        }

        binding.addCreditCardButton.addCreditCardLayout.setOnClickListener { interactor.onAddCreditCardClick() }
    }

    /**
     * Updates the display of the credit cards based on the given [CreditCardsListState].
     */
    fun update(state: CreditCardsListState) {
        binding.progressBar.isVisible = state.isLoading
        binding.creditCardsList.isVisible = state.creditCards.isNotEmpty()

        creditCardsAdapter.submitList(state.creditCards)
    }

    companion object {
        const val LAYOUT_ID = R.layout.component_credit_cards
    }
}
