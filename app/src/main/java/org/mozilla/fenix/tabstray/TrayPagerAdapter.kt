/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.browser.state.selector.normalTabs
import mozilla.components.browser.state.selector.privateTabs
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.store.BrowserStore
import org.mozilla.fenix.sync.SyncedTabsAdapter
import org.mozilla.fenix.tabstray.browser.BrowserTabsAdapter
import org.mozilla.fenix.tabstray.browser.BrowserTrayInteractor
import org.mozilla.fenix.tabstray.syncedtabs.TabClickDelegate
import org.mozilla.fenix.tabstray.viewholders.AbstractTrayViewHolder
import org.mozilla.fenix.tabstray.viewholders.NormalBrowserTabViewHolder
import org.mozilla.fenix.tabstray.viewholders.PrivateBrowserTabViewHolder
import org.mozilla.fenix.tabstray.viewholders.SyncedTabViewHolder

class TrayPagerAdapter(
    private val context: Context,
    private val store: TabsTrayStore,
    private val browserInteractor: BrowserTrayInteractor,
    private val navInteractor: NavigationInteractor,
    private val interactor: TabsTrayInteractor,
    private val browserStore: BrowserStore
) : RecyclerView.Adapter<AbstractTrayViewHolder>() {

    private val normalAdapter by lazy { BrowserTabsAdapter(context, browserInteractor, store) }
    private val privateAdapter by lazy { BrowserTabsAdapter(context, browserInteractor, store) }
    private val syncedTabsAdapter by lazy { SyncedTabsAdapter(TabClickDelegate(navInteractor)) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbstractTrayViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(viewType, parent, false)

        val selectedTab = browserStore.state.selectedTab

        return when (viewType) {
            NormalBrowserTabViewHolder.LAYOUT_ID -> {
                NormalBrowserTabViewHolder(
                    itemView,
                    store,
                    interactor,
                    browserStore.state.normalTabs.indexOf(selectedTab)
                )
            }
            PrivateBrowserTabViewHolder.LAYOUT_ID -> {
                PrivateBrowserTabViewHolder(
                    itemView,
                    store,
                    interactor,
                    browserStore.state.privateTabs.indexOf(selectedTab)
                )
            }
            SyncedTabViewHolder.LAYOUT_ID -> {
                SyncedTabViewHolder(
                    itemView,
                    store
                )
            }
            else -> throw IllegalStateException("Unknown viewType.")
        }
    }

    override fun onBindViewHolder(viewHolder: AbstractTrayViewHolder, position: Int) {
        val adapter = when (position) {
            POSITION_NORMAL_TABS -> normalAdapter
            POSITION_PRIVATE_TABS -> privateAdapter
            POSITION_SYNCED_TABS -> syncedTabsAdapter
            else -> throw IllegalStateException("View type does not exist.")
        }
        viewHolder.bind(adapter, browserInteractor.getLayoutManagerForPosition(context, position))
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            POSITION_NORMAL_TABS -> NormalBrowserTabViewHolder.LAYOUT_ID
            POSITION_PRIVATE_TABS -> PrivateBrowserTabViewHolder.LAYOUT_ID
            POSITION_SYNCED_TABS -> SyncedTabViewHolder.LAYOUT_ID
            else -> throw IllegalStateException("Unknown position.")
        }
    }

    override fun getItemCount(): Int = TRAY_TABS_COUNT

    companion object {
        const val TRAY_TABS_COUNT = 3

        val POSITION_NORMAL_TABS = Page.NormalTabs.ordinal
        val POSITION_PRIVATE_TABS = Page.PrivateTabs.ordinal
        val POSITION_SYNCED_TABS = Page.SyncedTabs.ordinal
    }
}
