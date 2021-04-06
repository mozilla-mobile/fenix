/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.concept.tabstray.Tab
import org.mozilla.fenix.R
import org.mozilla.fenix.selection.SelectionHolder
import org.mozilla.fenix.tabstray.TabsTrayInteractor
import org.mozilla.fenix.tabstray.TabsTrayStore
import org.mozilla.fenix.tabstray.browser.BrowserTabsAdapter

/**
 * View holder for the normal tabs tray list.
 */
class NormalBrowserTabViewHolder(
    private val store: TabsTrayStore,
    containerView: View,
    interactor: TabsTrayInteractor
) : BaseBrowserTabViewHolder(containerView, interactor), SelectionHolder<Tab> {

    /**
     * Holds the list of selected tabs.
     *
     * Implementation notes: we do this here because we only want the normal tabs list to be able
     * to select tabs.
     */
    override val selectedItems: Set<Tab>
        get() = store.state.mode.selectedTabs

    override fun bind(
        adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>,
        layoutManager: RecyclerView.LayoutManager
    ) {
        (adapter as BrowserTabsAdapter).selectionHolder = this

        super.bind(adapter, layoutManager)
    }

    companion object {
        const val LAYOUT_ID = R.layout.normal_browser_tray_list
    }
}
