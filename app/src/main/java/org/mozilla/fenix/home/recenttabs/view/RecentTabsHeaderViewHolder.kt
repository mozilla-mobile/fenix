/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.recenttabs.view

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.navigation.Navigation.findNavController
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.RecentTabsHeaderBinding
import org.mozilla.fenix.home.recenttabs.interactor.RecentTabInteractor
import org.mozilla.fenix.utils.view.ViewHolder

/**
 * View holder for the recent tabs header and "Show all" button.
 *
 * @param interactor [RecentTabInteractor] which will have delegated to all user interactions.
 */
class RecentTabsHeaderViewHolder(
    view: View,
    private val interactor: RecentTabInteractor
) : ViewHolder(view) {

    init {
        val binding = RecentTabsHeaderBinding.bind(view)
        binding.showAllButton.setOnClickListener {
            hideKeyboard(view)
            interactor.onRecentTabShowAllClicked()
        }
    }

    /**
     * Hide the keyboard if we are viewing the home screen from behind the search dialog.
     */
    private fun hideKeyboard(view: View) {
        if (findNavController(view).currentDestination?.id == R.id.searchDialogFragment) {
            val imm =
                view.context
                    .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
        }
    }

    companion object {
        const val LAYOUT_ID = R.layout.recent_tabs_header
    }
}
