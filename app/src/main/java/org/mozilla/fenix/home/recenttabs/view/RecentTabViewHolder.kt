/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.recenttabs.view

import android.view.View
import kotlinx.android.synthetic.main.recent_tabs_list_row.*
import mozilla.components.browser.state.state.TabSessionState
import org.mozilla.fenix.R
import org.mozilla.fenix.home.recenttabs.interactor.RecentTabInteractor
import org.mozilla.fenix.utils.view.ViewHolder

/**
 * View holder for a recent tab item.
 *
 * @param interactor [RecentTabInteractor] which will have delegated to all user interactions.
 */
class RecentTabViewHolder(
    view: View,
    private val interactor: RecentTabInteractor
) : ViewHolder(view) {

    fun bindTab(tab: TabSessionState) {
        recent_tab_title.text = tab.content.title
        recent_tab_icon.setImageBitmap(tab.content.icon)

        itemView.setOnClickListener {
            interactor.onRecentTabClicked(tab.id)
        }
    }

    companion object {
        const val LAYOUT_ID = R.layout.recent_tabs_list_row
    }
}
