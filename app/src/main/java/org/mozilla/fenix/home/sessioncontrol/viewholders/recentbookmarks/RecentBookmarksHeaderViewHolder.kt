/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol.viewholders.recentbookmarks

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.recent_bookmarks_header.view.*
import org.mozilla.fenix.R
import org.mozilla.fenix.home.sessioncontrol.SessionControlInteractor

class RecentBookmarksHeaderViewHolder(
    view: View,
    private val interactor: SessionControlInteractor
) : RecyclerView.ViewHolder(view) {

    private val recentlySavedBookmarksHeader = view.recentlySavedBookmarksHeader
    private val showAllBookmarksButton = view.showAllBookmarksButton

    init {
        view.showAllBookmarksButton.setOnClickListener {
            interactor.onShowAllBookmarksClicked()
        }
    }

    fun bind() {
        val context = itemView.context
        recentlySavedBookmarksHeader.text =
            context.getString(R.string.recently_saved_bookmarks)
        showAllBookmarksButton.text =
            context.getString(R.string.recently_saved_show_all)
    }

    companion object {
        const val LAYOUT_ID = R.layout.recent_bookmarks_header
    }
}
