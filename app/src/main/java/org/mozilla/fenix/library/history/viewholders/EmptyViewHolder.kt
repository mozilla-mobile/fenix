/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.HistoryListEmptyBinding
import org.mozilla.fenix.library.history.HistoryViewItem

class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val binding = HistoryListEmptyBinding.bind(view)
    private lateinit var item: HistoryViewItem.EmptyHistoryItem

    fun bind(item: HistoryViewItem.EmptyHistoryItem) {
        binding.emptyMessage.text = item.title
        this.item = item
    }

    companion object {
        const val LAYOUT_ID = R.layout.history_list_empty
    }
}
