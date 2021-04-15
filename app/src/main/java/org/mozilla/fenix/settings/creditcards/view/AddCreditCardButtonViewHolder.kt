package org.mozilla.fenix.settings.creditcards.view

import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.fenix.R
import kotlinx.android.synthetic.main.saved_cards_add_button.*
import org.mozilla.fenix.settings.creditcards.interactor.CreditCardsManagementInteractor

/**
 * Viewholder for Add Credit Card Button
 */
class AddCreditCardButtonViewHolder(
        private val view: View,
        private val interactor: CreditCardsManagementInteractor) : RecyclerView.ViewHolder(view) {

    fun bind() {
        Log.e("VIEWHOLDER VIEW",view.tag.toString())
        view.setOnClickListener {
            interactor.onAddCreditCard()
        }
    }

    companion object {
        const val LAYOUT_ID = R.layout.saved_cards_add_button
    }
}