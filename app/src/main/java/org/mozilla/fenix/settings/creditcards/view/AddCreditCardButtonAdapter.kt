package org.mozilla.fenix.settings.creditcards.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.fenix.settings.creditcards.interactor.CreditCardsManagementInteractor

/**
 * Adapter for the add credit card button
 */
class AddCreditCardButtonAdapter(
        private val interactor: CreditCardsManagementInteractor
) : RecyclerView.Adapter<AddCreditCardButtonViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddCreditCardButtonViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(AddCreditCardButtonViewHolder.LAYOUT_ID, parent, false)
        return AddCreditCardButtonViewHolder(view,interactor)
    }

    override fun onBindViewHolder(holder: AddCreditCardButtonViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount(): Int {
        return 1
    }

}