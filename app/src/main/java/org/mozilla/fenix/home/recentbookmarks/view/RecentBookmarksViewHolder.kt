/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.recentbookmarks.view

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL
import kotlinx.android.synthetic.main.component_recent_bookmarks.view.*
import kotlinx.android.synthetic.main.recent_bookmarks_header.*
import mozilla.components.concept.storage.BookmarkNode
import org.mozilla.fenix.R
import org.mozilla.fenix.home.recentbookmarks.RecentBookmarksItemAdapter
import org.mozilla.fenix.home.recentbookmarks.interactor.RecentBookmarksInteractor
import org.mozilla.fenix.utils.view.ViewHolder

class RecentBookmarksViewHolder(
    view: View,
    val interactor: RecentBookmarksInteractor
) : ViewHolder(view) {

    private val recentBookmarksAdapter = RecentBookmarksItemAdapter(interactor)

    init {
        val linearLayoutManager = LinearLayoutManager(view.context, HORIZONTAL, false)

        view.recent_bookmarks_list.apply {
            adapter = recentBookmarksAdapter
            layoutManager = linearLayoutManager
        }

        showAllBookmarksButton.setOnClickListener {
            hideKeyboard(view)
            interactor.onShowAllBookmarksClicked()
        }
    }

    fun bind(bookmarks: List<BookmarkNode>) {
        recentBookmarksAdapter.submitList(bookmarks)
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
        const val LAYOUT_ID = R.layout.component_recent_bookmarks
    }
}
