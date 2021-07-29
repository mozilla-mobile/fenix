/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.recentbookmarks.view

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.navigation.Navigation
import kotlinx.android.synthetic.main.recent_bookmark_item.bookmark_title
import kotlinx.android.synthetic.main.recent_bookmark_item.bookmark_subtitle
import kotlinx.android.synthetic.main.recent_bookmark_item.bookmark_item
import kotlinx.android.synthetic.main.recent_bookmark_item.favicon_image
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.support.ktx.kotlin.tryGetHostFromUrl
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.loadIntoView
import org.mozilla.fenix.home.recentbookmarks.interactor.RecentBookmarksInteractor
import org.mozilla.fenix.utils.view.ViewHolder

class RecentBookmarkItemViewHolder(
    private val view: View,
    private val interactor: RecentBookmarksInteractor
) : ViewHolder(view) {

    fun bind(bookmark: BookmarkNode) {
        bookmark_title.text = bookmark.title ?: bookmark.url
        bookmark_subtitle.text = bookmark.url?.tryGetHostFromUrl() ?: bookmark.title ?: ""

        bookmark_item.setOnClickListener {
            hideKeyboard(view)
            interactor.onRecentBookmarkClicked(bookmark)
        }

        bookmark.url?.let {
            view.context.components.core.icons.loadIntoView(favicon_image, it)
        }
    }

    /**
     * Hide the keyboard if we are viewing the home screen from behind the search dialog.
     */
    private fun hideKeyboard(view: View) {
        if (Navigation.findNavController(view).currentDestination?.id == R.id.searchDialogFragment) {
            val imm =
                view.context
                    .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
        }
    }

    companion object {
        const val LAYOUT_ID = R.layout.recent_bookmark_item
    }
}
