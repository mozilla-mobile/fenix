/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol.viewholders

import android.content.Context
import android.view.View
import androidx.core.graphics.BlendModeColorFilterCompat.createBlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat.SRC_IN
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.collection_home_list_row.*
import kotlinx.android.synthetic.main.component_top_sites.view.*
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.item.SimpleBrowserMenuItem
import mozilla.components.browser.state.selector.normalTabs
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.feature.tab.collections.TabCollection
import mozilla.components.feature.top.sites.TopSite
import org.mozilla.fenix.R
import org.mozilla.fenix.utils.view.ViewHolder
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getIconColor
import org.mozilla.fenix.ext.increaseTapArea
import org.mozilla.fenix.ext.removeAndDisable
import org.mozilla.fenix.ext.removeTouchDelegate
import org.mozilla.fenix.ext.showAndEnable
import org.mozilla.fenix.home.sessioncontrol.CollectionInteractor
import org.mozilla.fenix.home.sessioncontrol.viewholders.topsites.TopSitesAdapter
import org.mozilla.fenix.theme.ThemeManager
import org.mozilla.fenix.utils.AccessibilityGridLayoutManager

class RecentBookmarksViewHolder(
    view: View,
    val interactor: RecentBookmarksInteractor
) : ViewHolder(view) {

    private val recentBookmarksAdapter = RecentBookmarksAdapter(interactor)

    init {
        val linearLayoutManager = LinearLayoutManager(view.context)

        view.top_sites_list.apply {
            adapter = recentBookmarksAdapter
            layoutManager = linearLayoutManager
        }
    }

    fun bind(bookmarks: List<BookmarkNode>) {
        recentBookmarksAdapter.submitList(bookmarks)
    }

    companion object {
        const val LAYOUT_ID = R.layout.component_top_sites
    }
}
