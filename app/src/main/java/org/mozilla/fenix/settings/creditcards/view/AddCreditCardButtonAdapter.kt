/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.fenix.settings.creditcards.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.fenix.settings.creditcards.interactor.CreditCardsManagementInteractor

/**
 * Adapter for add credit card button to be displayed
 *
 * @param interactor The [interactor] that will navigate to credit card editor
 */
class AddCreditCardButtonAdapter(
    private val interactor: CreditCardsManagementInteractor
) : RecyclerView.Adapter<AddCreditCardButtonViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AddCreditCardButtonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(AddCreditCardButtonViewHolder.LAYOUT_ID, parent, false)
        return AddCreditCardButtonViewHolder(view, interactor)
    }

    override fun onBindViewHolder(holderButton: AddCreditCardButtonViewHolder, position: Int) {
        holderButton.bind()
    }

    override fun getItemCount(): Int {
        return 1
    }
}
