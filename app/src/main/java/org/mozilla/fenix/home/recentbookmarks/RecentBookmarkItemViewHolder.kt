/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.recentbookmarks

import android.view.View
import kotlinx.android.synthetic.main.recent_bookmark_item.*
import kotlinx.android.synthetic.main.recent_bookmark_item.favicon_image
import mozilla.components.concept.storage.BookmarkNode
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.loadIntoView
import org.mozilla.fenix.home.recentbookmarks.interactor.DefaultRecentBookmarksInteractor
import org.mozilla.fenix.utils.view.ViewHolder

class RecentBookmarkItemViewHolder(
    view: View,
    private val interactor: DefaultRecentBookmarksInteractor
) : ViewHolder(view) {

    fun bind(bookmark: BookmarkNode) {
        bookmark_title.text = bookmark.title
        bookmark_subtitle.text = bookmark.url
        bookmark_item.setOnClickListener {
            interactor.onRecentBookmarkClicked(bookmark)
        }

        bookmark.url?.let { itemView.context.components.core.icons.loadIntoView(favicon_image, it) }
    }

    companion object {
        const val LAYOUT_ID = R.layout.recent_bookmark_item
    }
}
