/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history.viewholders

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.RecentlyClosedNavItemBinding
import org.mozilla.fenix.databinding.SyncedHistoryNavItemBinding
import org.mozilla.fenix.library.history.HistoryInteractor
import org.mozilla.fenix.library.history.HistoryViewItem

class SyncedHistoryViewHolder(
    view: View,
    private val historyInteractor: HistoryInteractor
) : RecyclerView.ViewHolder(view) {

    private val binding = SyncedHistoryNavItemBinding.bind(view)
    private lateinit var item: HistoryViewItem.SyncedHistoryItem

    init {
        binding.root.setOnClickListener {
            historyInteractor.onSyncedHistoryClicked()
        }
        binding.syncedHistoryNav.isVisible = true
    }

    fun bind(item: HistoryViewItem.SyncedHistoryItem) {
        binding.syncedHistoryHeader.text = item.title
        this.item = item
    }

    companion object {
        const val LAYOUT_ID = R.layout.synced_history_nav_item
    }
}
