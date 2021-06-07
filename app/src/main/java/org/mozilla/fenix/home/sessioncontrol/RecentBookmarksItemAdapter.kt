/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import mozilla.components.concept.storage.BookmarkNode
import org.mozilla.fenix.home.sessioncontrol.viewholders.recentbookmarks.RecentBookmarkItemViewHolder

/**
 * Adapter for the individual bookmark items that will be used in [RecentBookmarksAdapter].
 */
class RecentBookmarksItemAdapter(
    private val interactor: SessionControlInteractor
) : ListAdapter<BookmarkNode, RecentBookmarkItemViewHolder>(RecentBookmarksDiffCallback) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecentBookmarkItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(RecentBookmarkItemViewHolder.LAYOUT_ID, parent, false)
        return RecentBookmarkItemViewHolder(view, interactor)
    }

    override fun onBindViewHolder(holder: RecentBookmarkItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(
        holder: RecentBookmarkItemViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNullOrEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            when (payloads[0]) {
                is BookmarkNode -> {
                    holder.bind((payloads[0] as BookmarkNode))
                }
            }
        }
    }

    data class RecentBookmarkItemPayload(
        val newInstance: BookmarkNode
    )

    internal object RecentBookmarksDiffCallback : DiffUtil.ItemCallback<BookmarkNode>() {
        override fun areItemsTheSame(oldItem: BookmarkNode, newItem: BookmarkNode) =
            oldItem.guid == newItem.guid

        override fun areContentsTheSame(oldItem: BookmarkNode, newItem: BookmarkNode) =
            oldItem.guid == newItem.guid &&
                    oldItem.parentGuid == newItem.parentGuid &&
                    oldItem.title == newItem.title &&
                    oldItem.url == newItem.url &&
                    oldItem.type == newItem.type
    }
}
