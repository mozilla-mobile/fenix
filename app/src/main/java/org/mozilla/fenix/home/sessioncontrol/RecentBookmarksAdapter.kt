/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import kotlinx.android.synthetic.main.component_bookmark.view.*
import mozilla.components.concept.storage.BookmarkNode
import org.mozilla.fenix.home.sessioncontrol.viewholders.recentbookmarks.RecentBookmarksViewHolder

class RecentBookmarksAdapter(
    private val interactor: SessionControlInteractor
) : ListAdapter<List<BookmarkNode>, RecentBookmarksViewHolder>(RecentBookmarksListDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentBookmarksViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(RecentBookmarksViewHolder.LAYOUT_ID, parent, false)
        return RecentBookmarksViewHolder(view, interactor)
    }

    override fun onBindViewHolder(
        holder: RecentBookmarksViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNullOrEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            if (payloads[0] is AdapterItem.RecentBookmarksPayload) {
                val adapter = holder.itemView.bookmark_list.adapter as RecentBookmarksItemAdapter
                val payload = payloads[0] as AdapterItem.RecentBookmarksPayload
                for (item in payload.changed) {
                    adapter.notifyItemChanged(
                        item.first % RecentBookmarksViewHolder.MAX_BOOKMARKS,
                        RecentBookmarksItemAdapter.RecentBookmarkItemPayload(item.second)
                    )
                }
            }
        }
    }

    override fun onBindViewHolder(holder: RecentBookmarksViewHolder, position: Int) {
        val adapter = holder.itemView.bookmark_list.adapter as RecentBookmarksItemAdapter
        adapter.submitList(getItem(position))
    }

    internal object RecentBookmarksListDiffCallback : DiffUtil.ItemCallback<List<BookmarkNode>>() {
        override fun areItemsTheSame(
            oldItem: List<BookmarkNode>,
            newItem: List<BookmarkNode>
        ): Boolean {
            return oldItem.size == newItem.size
        }

        override fun areContentsTheSame(
            oldItem: List<BookmarkNode>,
            newItem: List<BookmarkNode>
        ): Boolean {
            return newItem.zip(oldItem).all { (new, old) -> new == old }
        }

        override fun getChangePayload(
            oldItem: List<BookmarkNode>,
            newItem: List<BookmarkNode>
        ): Any? {
            val changed = mutableSetOf<Pair<Int, BookmarkNode>>()
            for ((index, item) in newItem.withIndex()) {
                if (oldItem.getOrNull(index) != item) {
                    changed.add(Pair(index, item))
                }
            }
            return if (changed.isNotEmpty()) AdapterItem.RecentBookmarksPayload(changed) else null
        }
    }
}
