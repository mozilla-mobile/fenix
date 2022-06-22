/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history.viewholders

import android.view.View
import androidx.core.view.isGone
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.HistoryListHistoryBinding
import org.mozilla.fenix.ext.hideAndDisable
import org.mozilla.fenix.ext.showAndEnable
import org.mozilla.fenix.library.history.History
import org.mozilla.fenix.library.history.HistoryFragmentState
import org.mozilla.fenix.library.history.HistoryInteractor
import org.mozilla.fenix.library.history.HistoryViewItem
import org.mozilla.fenix.selection.SelectionHolder

class HistoryViewHolder(
    view: View,
    private val historyInteractor: HistoryInteractor,
    private val selectionHolder: SelectionHolder<History>,
    private val onDeleteClicked: (Int) -> Unit
) : RecyclerView.ViewHolder(view) {

    private lateinit var item: HistoryViewItem.HistoryItem
    private val binding = HistoryListHistoryBinding.bind(view)

    init {
        binding.historyLayout.overflowView.apply {
            setImageResource(R.drawable.ic_close)
            contentDescription = view.context.getString(R.string.history_delete_item)
            setOnClickListener {
                onDeleteClicked.invoke(bindingAdapterPosition)
            }
        }
    }

    fun bind(
        item: HistoryViewItem.HistoryItem,
        mode: HistoryFragmentState.Mode
    ) {
        binding.historyLayout.titleView.text = item.data.title
        binding.historyLayout.urlView.text = item.data.url

        binding.historyLayout.setSelectionInteractor(item.data, selectionHolder, historyInteractor)
        binding.historyLayout.changeSelected(item.data in selectionHolder.selectedItems)

        if (!this::item.isInitialized || this.item.data.url != item.data.url) {
            binding.historyLayout.loadFavicon(item.data.url)
        }

        if (mode is HistoryFragmentState.Mode.Editing) {
            binding.historyLayout.overflowView.hideAndDisable()
        } else {
            binding.historyLayout.overflowView.showAndEnable()
        }

        this.item = item
    }

    companion object {
        const val DISABLED_BUTTON_ALPHA = 0.7f
        const val LAYOUT_ID = R.layout.history_list_history
    }
}
