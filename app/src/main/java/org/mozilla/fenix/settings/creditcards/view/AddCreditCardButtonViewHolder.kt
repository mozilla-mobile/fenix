/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.creditcards.view

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.fenix.R
import org.mozilla.fenix.settings.creditcards.interactor.CreditCardsManagementInteractor

/**
 * View holer for add credit card button
 *
 * @param view view to set onClickListener can navigate to credit card editor
 * @param interactor interactor to call onAddCreditCard
 */
class AddCreditCardButtonViewHolder(
    private val view: View,
    private val interactor: CreditCardsManagementInteractor
) : RecyclerView.ViewHolder(view) {

    fun bind() {
        view.setOnClickListener {
            interactor.onAddCreditCard()
        }
    }

    companion object {
        const val LAYOUT_ID = R.layout.credit_card_add_button
    }
}
