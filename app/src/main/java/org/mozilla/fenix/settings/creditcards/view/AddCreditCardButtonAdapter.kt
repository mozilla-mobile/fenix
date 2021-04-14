package org.mozilla.fenix.settings.creditcards.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for the add credit card button
 */
class AddCreditCardButtonAdapter : RecyclerView.Adapter<AddCreditCardButtonViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddCreditCardButtonViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(CreditCardItemViewHolder.LAYOUT_ID, parent, false)
        return AddCreditCardButtonViewHolder(view)
    }

    override fun onBindViewHolder(holder: AddCreditCardButtonViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return 1
    }

}