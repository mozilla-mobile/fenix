/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.browser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.fenix.FeatureFlags
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.TabTrayTitleHeaderItemBinding

/**
 * A [RecyclerView.Adapter] for tab header.
 *
 * @param title [String] used for the title
 */
class TitleHeaderAdapter(val title: String) : RecyclerView.Adapter<TitleHeaderAdapter.HeaderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return HeaderViewHolder(view, title)
    }

    override fun getItemViewType(position: Int): Int {
        return HeaderViewHolder.LAYOUT_ID
    }

    override fun getItemCount(): Int {
        return if (FeatureFlags.tabGroupFeature) {
            1
        } else {
            0
        }
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        /* Do nothing */
    }

    class HeaderViewHolder(
        itemView: View,
        val title: String
    ) : RecyclerView.ViewHolder(itemView) {
        private val binding = TabTrayTitleHeaderItemBinding.bind(itemView)

        fun bind() {
            binding.tabTrayHeaderTitle.text = title
        }

        companion object {
            const val LAYOUT_ID = R.layout.tab_tray_title_header_item
        }
    }
}
