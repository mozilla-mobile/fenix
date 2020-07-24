/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabtray

import android.content.Context
import android.view.LayoutInflater
import mozilla.components.browser.tabstray.TabViewHolder
import mozilla.components.browser.tabstray.TabsAdapter
import mozilla.components.concept.tabstray.Tabs
import mozilla.components.support.images.loader.ImageLoader
import org.mozilla.fenix.R

class FenixTabsAdapter(
    context: Context,
    imageLoader: ImageLoader
) : TabsAdapter(
    viewHolderProvider = { parentView ->
        TabTrayViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.tab_tray_item,
                parentView,
                false),
            imageLoader
        )
    }
) {
    var onTabsUpdated: (() -> Unit)? = null
    var tabCount = 0

    override fun updateTabs(tabs: Tabs) {
        super.updateTabs(tabs)
        onTabsUpdated?.invoke()
        tabCount = tabs.list.size
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val newIndex = tabCount - position - 1
        (holder as TabTrayViewHolder).updateAccessibilityRowIndex(holder.itemView, newIndex)
    }
}
