package org.mozilla.fenix.settings.creditcards.view

import android.annotation.SuppressLint
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.fenix.R
import org.mozilla.fenix.settings.creditcards.interactor.CreditCardsManagementInteractor

class AddCreditCardButtonViewHolder(
    private val view : View,
    private val interactor: CreditCardsManagementInteractor) : RecyclerView.ViewHolder(view){

    @SuppressLint("ResourceType")
    fun bind() {
        view.findViewById<ConstraintLayout>(LAYOUT_ID).setOnClickListener {
            //Add clicking code from interactor
            interactor.onAddCreditCard()
        }
    }

    companion object {
        const val LAYOUT_ID = R.layout.credit_card_add_button
    }
}