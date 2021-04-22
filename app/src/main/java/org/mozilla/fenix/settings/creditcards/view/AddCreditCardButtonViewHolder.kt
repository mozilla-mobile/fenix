package org.mozilla.fenix.settings.creditcards.view

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.fenix.R
import org.mozilla.fenix.settings.creditcards.interactor.CreditCardsManagementInteractor

/**
 * Viewholder for Add Credit Card Button
 */
class AddCreditCardButtonViewHolder(
        private val view: View,
        private val interactor: CreditCardsManagementInteractor) : RecyclerView.ViewHolder(view) {

    fun bind() {
        view.setOnClickListener {
            interactor.onAddCreditCard()
        }
    }

    companion object {
        const val LAYOUT_ID = R.layout.saved_cards_add_button
    }
}