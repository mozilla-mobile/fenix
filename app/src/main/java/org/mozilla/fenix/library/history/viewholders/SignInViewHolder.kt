package org.mozilla.fenix.library.history.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.HistoryListSignInBinding
import org.mozilla.fenix.library.history.HistoryViewItem

class SignInViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val binding = HistoryListSignInBinding.bind(view)
    private lateinit var item: HistoryViewItem.SignInHistoryItem

    fun bind(item: HistoryViewItem.SignInHistoryItem) {
        binding.signInMessage.text = item.instructionText
        this.item = item
    }

    companion object {
        const val LAYOUT_ID = R.layout.history_list_sign_in
    }
}