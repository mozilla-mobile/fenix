/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history.viewholders

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.RecentlyClosedNavItemBinding
import org.mozilla.fenix.library.history.HistoryInteractor
import org.mozilla.fenix.library.history.HistoryViewItem

class RecentlyClosedViewHolder(
    view: View,
    private val historyInteractor: HistoryInteractor
) : RecyclerView.ViewHolder(view) {

    private val binding = RecentlyClosedNavItemBinding.bind(view)
    private lateinit var item: HistoryViewItem.RecentlyClosedItem

    init {
        binding.root.setOnClickListener {
            historyInteractor.onRecentlyClosedClicked()
        }
        binding.recentlyClosedNav.isVisible = true
    }

    fun bind(item: HistoryViewItem.RecentlyClosedItem) {
        binding.recentlyClosedTabsHeader.text = item.title
        binding.recentlyClosedTabsDescription.text = item.body
        this.item = item
    }

    companion object {
        const val LAYOUT_ID = R.layout.recently_closed_nav_item
    }
}
