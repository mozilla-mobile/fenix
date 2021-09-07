/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.browser

import TabGroupViewHolder
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import mozilla.components.concept.tabstray.Tabs
import mozilla.components.concept.tabstray.Tab as TabsTrayTab
import mozilla.components.concept.tabstray.TabsTray
import mozilla.components.support.base.observer.ObserverRegistry
import org.mozilla.fenix.components.Components
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.tabstray.TabsTrayStore
import kotlin.math.max
import mozilla.components.support.base.observer.Observable as ComponentObservable

/**
 * The [ListAdapter] for displaying the list of search term tabs.
 *
 * @param context [Context] used for various platform interactions or accessing [Components]
 * @param browserTrayInteractor [BrowserTrayInteractor] handling tabs interactions in a tab tray.
 * @param featureName [String] representing the name of the feature displaying tabs. Used in telemetry reporting.
 * @param delegate [Observable]<[TabsTray.Observer]> for observing tabs tray changes. Defaults to [ObserverRegistry].
 */
class TabGroupAdapter(
    private val context: Context,
    private val browserTrayInteractor: BrowserTrayInteractor,
    private val store: TabsTrayStore,
    private val featureName: String,
    delegate: ComponentObservable<TabsTray.Observer> = ObserverRegistry()
) : ListAdapter<TabGroupAdapter.Group, TabGroupViewHolder>(DiffCallback),
    TabsTray,
    ComponentObservable<TabsTray.Observer> by delegate {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabGroupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)

        return when {
            context.components.settings.gridTabView -> {
                TabGroupViewHolder(view, HORIZONTAL)
            }
            else -> {
                TabGroupViewHolder(view, VERTICAL)
            }
        }
    }

    override fun onBindViewHolder(holder: TabGroupViewHolder, position: Int) {
        val group = getItem(position)
        holder.bind(group, browserTrayInteractor, store, this)
    }

    override fun getItemViewType(position: Int): Int {
        return TabGroupViewHolder.LAYOUT_ID
    }

    override fun updateTabs(tabs: Tabs) {
        val data = tabs.list.groupBy { it.searchTerm.lowercase() }

        val grouping = data.map { mapEntry ->
            val searchTerm = mapEntry.key.replaceFirstChar(Char::uppercase)
            val groupTabs = mapEntry.value
            val groupMax = groupTabs.fold(0L) { acc, tab ->
                max(tab.lastAccess, acc)
            }

            Group(
                title = searchTerm,
                tabs = groupTabs,
                lastAccess = groupMax
            )
        }.sortedBy { it.lastAccess }

        submitList(grouping)
    }

    data class Group(
        /**
         * A title for the tab group.
         */
        val title: String,

        /**
         * The list of tabs belonging to this tab group.
         */
        val tabs: List<TabsTrayTab>,

        /**
         * The last time tabs in this group was accessed.
         */
        val lastAccess: Long
    )

    override fun isTabSelected(tabs: Tabs, position: Int): Boolean =
        tabs.selectedIndex == position
    override fun onTabsChanged(position: Int, count: Int) = Unit
    override fun onTabsInserted(position: Int, count: Int) = Unit
    override fun onTabsMoved(fromPosition: Int, toPosition: Int) = Unit
    override fun onTabsRemoved(position: Int, count: Int) = Unit

    private object DiffCallback : DiffUtil.ItemCallback<Group>() {
        override fun areItemsTheSame(oldItem: Group, newItem: Group): Boolean {
            return oldItem.title == newItem.title
        }

        override fun areContentsTheSame(oldItem: Group, newItem: Group): Boolean {
            return oldItem == newItem
        }
    }
}
