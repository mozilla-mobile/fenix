/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.recentbookmarks.controller

import androidx.navigation.NavController
import mozilla.appservices.places.BookmarkRoot
import mozilla.components.concept.storage.BookmarkNode
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.home.HomeFragmentDirections
import org.mozilla.fenix.home.recentbookmarks.interactor.RecentBookmarksInteractor

/**
 * An interface that handles the view manipulation of the recently saved bookmarks on the
 * Home screen.
 */
interface RecentBookmarksController {

    /**
     * @see [RecentBookmarksInteractor.onRecentBookmarkClicked]
     */
    fun handleBookmarkClicked(bookmark: BookmarkNode)

    /**
     * @see [RecentBookmarksInteractor.onShowAllBookmarksClicked]
     */
    fun handleShowAllBookmarksClicked()
}

/**
 * The default implementation of [RecentBookmarksController].
 */
class DefaultRecentBookmarksController(
    private val activity: HomeActivity,
    private val navController: NavController
) : RecentBookmarksController {

    /**
     * @see [RecentBookmarksController.handleBookmarkClicked]
     */
    override fun handleBookmarkClicked(bookmark: BookmarkNode) {
        with(activity) {
            browsingModeManager.mode = browsingModeManager.mode
            openToBrowserAndLoad(
                searchTermOrURL = bookmark.url!!,
                newTab = true,
                from = BrowserDirection.FromHome
            )
        }
    }

    /**
     * @see [RecentBookmarksController.handleShowAllBookmarksClicked]
     */
    override fun handleShowAllBookmarksClicked() {
        val directions = HomeFragmentDirections.actionGlobalBookmarkFragment(BookmarkRoot.Mobile.id)
        navController.nav(R.id.homeFragment, directions)
    }
}
