/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.browser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.browser.state.store.BrowserStore
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.TabTrayTitleHeaderItemBinding

/**
 * A [RecyclerView.Adapter] for tab header.
 *
 * @param title [String] used for the title
 */
class TitleHeaderAdapter(
    private val browserStore: BrowserStore,
    @StringRes val title: Int
) : ListAdapter<TitleHeaderAdapter.Header, TitleHeaderAdapter.HeaderViewHolder>(DiffCallback) {

    object Header

    private val normalTabsHeaderBinding = TitleHeaderBinding(browserStore, ::handleListChanges)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return HeaderViewHolder(view, title)
    }

    override fun getItemViewType(position: Int) = HeaderViewHolder.LAYOUT_ID

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        normalTabsHeaderBinding.start()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        normalTabsHeaderBinding.stop()
    }

    /* Do nothing */
    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) = Unit

    private fun handleListChanges(showHeader: Boolean) {
        val header = if (showHeader) {
            listOf(Header)
        } else {
            emptyList()
        }

        submitList(header)
    }

    class HeaderViewHolder(
        itemView: View,
        @StringRes val title: Int
    ) : RecyclerView.ViewHolder(itemView) {
        private val binding = TabTrayTitleHeaderItemBinding.bind(itemView)

        fun bind() {
            binding.tabTrayHeaderTitle.text = itemView.context.getString(title)
        }

        companion object {
            const val LAYOUT_ID = R.layout.tab_tray_title_header_item
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Header>() {
        override fun areItemsTheSame(oldItem: Header, newItem: Header) = true
        override fun areContentsTheSame(oldItem: Header, newItem: Header) = true
    }
}
