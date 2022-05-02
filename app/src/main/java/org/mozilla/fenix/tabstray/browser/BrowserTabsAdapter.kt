/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.browser

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.tabstray.TabsAdapter.Companion.PAYLOAD_DONT_HIGHLIGHT_SELECTED_ITEM
import mozilla.components.browser.tabstray.TabsAdapter.Companion.PAYLOAD_HIGHLIGHT_SELECTED_ITEM
import mozilla.components.browser.thumbnails.loader.ThumbnailLoader
import org.mozilla.fenix.components.Components
import org.mozilla.fenix.databinding.TabTrayGridItemBinding
import org.mozilla.fenix.databinding.TabTrayItemBinding
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.selection.SelectionHolder
import org.mozilla.fenix.tabstray.TabsTrayStore

/**
 * A [RecyclerView.Adapter] for browser tabs.
 *
 * @param context [Context] used for various platform interactions or accessing [Components]
 * @param interactor [BrowserTrayInteractor] handling tabs interactions in a tab tray.
 * @param store [TabsTrayStore] containing the complete state of tabs tray and methods to update that.
 * @param featureName [String] representing the name of the feature displaying tabs. Used in telemetry reporting.
 */
class BrowserTabsAdapter(
    private val context: Context,
    val interactor: BrowserTrayInteractor,
    private val store: TabsTrayStore,
    override val featureName: String
) : TabsAdapter<AbstractBrowserTabViewHolder>(interactor), FeatureNameHolder {

    /**
     * The layout types for the tabs.
     */
    enum class ViewType(val layoutRes: Int) {
        LIST(BrowserTabViewHolder.ListViewHolder.LAYOUT_ID),
        GRID(BrowserTabViewHolder.GridViewHolder.LAYOUT_ID)
    }

    /**
     * Tracks the selected tabs in multi-select mode.
     */
    var selectionHolder: SelectionHolder<TabSessionState>? = null

    private val selectedItemAdapterBinding = SelectedItemAdapterBinding(store, this)
    private val imageLoader = ThumbnailLoader(context.components.core.thumbnailStorage)

    override fun getItemViewType(position: Int): Int {
        return when {
            context.components.settings.gridTabView -> {
                ViewType.GRID.layoutRes
            }
            else -> {
                ViewType.LIST.layoutRes
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbstractBrowserTabViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)

        return when (viewType) {
            ViewType.GRID.layoutRes ->
                BrowserTabViewHolder.GridViewHolder(imageLoader, interactor, store, selectionHolder, view, featureName)
            else ->
                BrowserTabViewHolder.ListViewHolder(imageLoader, interactor, store, selectionHolder, view, featureName)
        }
    }

    override fun onBindViewHolder(holder: AbstractBrowserTabViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        var selectedMaskView: View? = null
        holder.tab?.let { tab ->
            when (getItemViewType(position)) {
                ViewType.GRID.layoutRes -> {
                    val gridBinding = TabTrayGridItemBinding.bind(holder.itemView)
                    selectedMaskView = gridBinding.checkboxInclude.selectedMask
                    gridBinding.mozacBrowserTabstrayClose.setOnClickListener { interactor.close(tab, featureName) }
                }
                ViewType.LIST.layoutRes -> {
                    val listBinding = TabTrayItemBinding.bind(holder.itemView)
                    selectedMaskView = listBinding.checkboxInclude.selectedMask
                    listBinding.mozacBrowserTabstrayClose.setOnClickListener { interactor.close(tab, featureName) }
                }
            }

            selectionHolder?.let {
                holder.showTabIsMultiSelectEnabled(
                    selectedMaskView,
                    (it.selectedItems.map { item -> item.id }).contains(tab.id)
                )
            }
        }
    }

    /**
     * Over-ridden [onBindViewHolder] that uses the payloads to notify the selected tab how to
     * display itself.
     */
    override fun onBindViewHolder(holder: AbstractBrowserTabViewHolder, position: Int, payloads: List<Any>) {
        if (currentList.isEmpty()) return

        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
            return
        }

        val tab = getItem(position)
        if (tab.id == selectedTabId) {
            if (payloads.contains(PAYLOAD_HIGHLIGHT_SELECTED_ITEM)) {
                holder.updateSelectedTabIndicator(true)
            } else if (payloads.contains(PAYLOAD_DONT_HIGHLIGHT_SELECTED_ITEM)) {
                holder.updateSelectedTabIndicator(false)
            }
        }

        selectionHolder?.let {
            var selectedMaskView: View? = null
            when (getItemViewType(position)) {
                ViewType.GRID.layoutRes -> {
                    val gridBinding = TabTrayGridItemBinding.bind(holder.itemView)
                    selectedMaskView = gridBinding.checkboxInclude.selectedMask
                }
                ViewType.LIST.layoutRes -> {
                    val listBinding = TabTrayItemBinding.bind(holder.itemView)
                    selectedMaskView = listBinding.checkboxInclude.selectedMask
                }
            }
            holder.showTabIsMultiSelectEnabled(
                selectedMaskView,
                it.selectedItems.map { item -> item.id }.contains(tab.id)
            )
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        selectedItemAdapterBinding.start()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        selectedItemAdapterBinding.stop()
    }
}
