package org.mozilla.fenix.settings.creditcards.view

import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.fenix.R
import org.mozilla.fenix.settings.creditcards.interactor.CreditCardsManagementInteractor

/**
 * Viewholder for Add Credit Card Button
 */
class AddCreditCardButtonViewHolder(
        private val view: View) : RecyclerView.ViewHolder(view) {

    fun bind(position : Int) {
        view.findViewById<ConstraintLayout>(R.id.saved_cards_layout).setOnClickListener {
            Log.e("Add Credit Card Viewhol","clicked")
        }
    }


    companion object {
        const val LAYOUT_ID = R.layout.saved_cards_add_button
    }

}