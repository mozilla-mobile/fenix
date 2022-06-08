/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.HistoryListHeaderBinding
import org.mozilla.fenix.library.history.HistoryInteractor
import org.mozilla.fenix.library.history.HistoryViewItem

class TimeGroupViewHolder(
    view: View,
    private val historyInteractor: HistoryInteractor
) : RecyclerView.ViewHolder(view) {

    private val binding = HistoryListHeaderBinding.bind(view)
    private lateinit var item: HistoryViewItem.TimeGroupHeader

    init {
        binding.root.setOnClickListener {
            historyInteractor.onTimeGroupClicked(item.timeGroup, item.collapsed)
        }
    }

    fun bind(item: HistoryViewItem.TimeGroupHeader, isCollapsed: Boolean) {
        binding.headerTitle.text = item.title
        binding.chevron.isActivated = isCollapsed
        this.item = item
    }

    companion object {
        const val LAYOUT_ID = R.layout.history_list_header
    }
}