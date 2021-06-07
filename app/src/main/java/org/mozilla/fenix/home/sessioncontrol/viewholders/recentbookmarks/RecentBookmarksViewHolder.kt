/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol.viewholders.recentbookmarks

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.component_recent_bookmarks.view.*
import mozilla.components.concept.storage.BookmarkNode
import org.mozilla.fenix.R
import org.mozilla.fenix.home.sessioncontrol.RecentBookmarksAdapter
import org.mozilla.fenix.home.sessioncontrol.SessionControlInteractor
import org.mozilla.fenix.utils.view.ViewHolder

class RecentBookmarksViewHolder(
    view: View,
    val interactor: SessionControlInteractor
) : ViewHolder(view) {

    private val recentBookmarksAdapter = RecentBookmarksAdapter(interactor)

    init {
        val linearLayoutManager = LinearLayoutManager(view.context)

        view.recent_bookmarks_list.apply {
            adapter = recentBookmarksAdapter
            layoutManager = linearLayoutManager
        }
    }

    fun bind(bookmarks: List<BookmarkNode>) {
        recentBookmarksAdapter.submitList(listOf(bookmarks))
    }

    companion object {
        const val LAYOUT_ID = R.layout.component_recent_bookmarks
        const val MAX_BOOKMARKS = 4
    }
}
