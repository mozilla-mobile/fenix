/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.browser

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import mozilla.components.browser.tabstray.TabsTrayStyling
import mozilla.components.browser.thumbnails.loader.ThumbnailLoader
import mozilla.components.concept.tabstray.Tab
import mozilla.components.concept.tabstray.TabsTray
import mozilla.components.support.base.observer.Observable
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.TabTrayGridItemBinding
import org.mozilla.fenix.databinding.TabTrayItemBinding
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.home.sessioncontrol.viewholders.topsites.dpToPx
import org.mozilla.fenix.tabstray.TabsTrayStore
import org.mozilla.fenix.tabstray.ext.MIN_COLUMN_WIDTH_DP

/**
 * The [ListAdapter] for displaying the list of tabs that have the same search term.
 *
 * @param context [Context] used for various platform interactions or accessing [Components]
 * @param browserTrayInteractor [BrowserTrayInteractor] handling tabs interactions in a tab tray.
 * @param store [TabsTrayStore] containing the complete state of tabs tray and methods to update that.
 * @param delegate [Observable]<[TabsTray.Observer]> for observing tabs tray changes. Defaults to [ObserverRegistry].
 * @param featureName [String] representing the name of the feature displaying tabs. Used in telemetry reporting.
 */
class TabGroupListAdapter(
    private val context: Context,
    private val interactor: BrowserTrayInteractor,
    private val store: TabsTrayStore,
    private val delegate: Observable<TabsTray.Observer>,
    private val featureName: String,
) : ListAdapter<Tab, AbstractBrowserTabViewHolder>(DiffCallback) {

    private val imageLoader = ThumbnailLoader(context.components.core.thumbnailStorage)
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AbstractBrowserTabViewHolder {
        val view = when {
            context.components.settings.gridTabView -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.tab_tray_grid_item, parent, false)
                view.layoutParams.width = view.dpToPx(MIN_COLUMN_WIDTH_DP.toFloat())
                view
            }
            else -> {
                LayoutInflater.from(parent.context).inflate(R.layout.tab_tray_item, parent, false)
            }
        }

        return BrowserTabViewHolder.ListViewHolder(imageLoader, interactor, store, null, view, featureName)
    }

    override fun onBindViewHolder(holder: AbstractBrowserTabViewHolder, position: Int) {
        val tab = getItem(position)
        holder.bind(tab, false, TabsTrayStyling(), delegate)
        holder.tab?.let { holderTab ->
            when {
                context.components.settings.gridTabView -> {
                    val gridBinding = TabTrayGridItemBinding.bind(holder.itemView)
                    gridBinding.mozacBrowserTabstrayClose.setOnClickListener {
                        interactor.close(holderTab, featureName)
                    }
                }
                else -> {
                    val listBinding = TabTrayItemBinding.bind(holder.itemView)
                    listBinding.mozacBrowserTabstrayClose.setOnClickListener {
                        interactor.close(holderTab, featureName)
                    }
                }
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Tab>() {
        override fun areItemsTheSame(oldItem: Tab, newItem: Tab): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Tab, newItem: Tab): Boolean {
            return oldItem == newItem
        }
    }
}
