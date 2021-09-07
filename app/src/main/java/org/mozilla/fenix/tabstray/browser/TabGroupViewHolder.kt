/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.concept.tabstray.TabsTray
import mozilla.components.support.base.observer.Observable
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.TabGroupItemBinding
import org.mozilla.fenix.tabstray.TabsTrayStore
import org.mozilla.fenix.tabstray.TrayPagerAdapter
import org.mozilla.fenix.tabstray.browser.BrowserTrayInteractor
import org.mozilla.fenix.tabstray.browser.TabGroupAdapter
import org.mozilla.fenix.tabstray.browser.TabGroupListAdapter

/**
 * A RecyclerView ViewHolder implementation for tab group items.
 *
 * @param itemView [View] that displays a "tab".
 * @param orientation [Int] orientation of the items.  Horizontal for grid layout, vertical for list layout
 */
class TabGroupViewHolder(
    itemView: View,
    val orientation: Int
) : RecyclerView.ViewHolder(itemView) {
    private val binding = TabGroupItemBinding.bind(itemView)

    fun bind(
        group: TabGroupAdapter.Group,
        interactor: BrowserTrayInteractor,
        store: TabsTrayStore,
        observable: Observable<TabsTray.Observer>
    ) {
        // bind title
        binding.tabGroupTitle.text = group.title
        // bind recyclerview for search term adapter
        binding.tabGroupList.apply {
            val groupListAdapter = TabGroupListAdapter(
                itemView.context, interactor, store, observable, TrayPagerAdapter.TAB_GROUP_FEATURE_NAME
            )
            layoutManager = LinearLayoutManager(itemView.context, orientation, false)
            adapter = groupListAdapter

            groupListAdapter.submitList(group.tabs)
        }
    }
    companion object {
        const val LAYOUT_ID = R.layout.tab_group_item
    }
}
