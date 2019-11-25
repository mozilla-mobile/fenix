/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.bookmarks

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Resources
import androidx.core.content.getSystemService
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import mozilla.components.concept.engine.prompt.ShareData
import mozilla.components.concept.storage.BookmarkNode
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.FenixSnackbarPresenter
import org.mozilla.fenix.components.Services
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.nav

/**
 * [BookmarkFragment] controller.
 * Delegated by View Interactors, handles container business logic and operates changes on it.
 */
@SuppressWarnings("TooManyFunctions")
interface BookmarkController {
    fun handleBookmarkTapped(item: BookmarkNode)
    fun handleBookmarkExpand(folder: BookmarkNode)
    fun handleSelectionModeSwitch()
    fun handleBookmarkEdit(node: BookmarkNode)
    fun handleBookmarkSelected(node: BookmarkNode)
    fun handleCopyUrl(item: BookmarkNode)
    fun handleBookmarkSharing(item: BookmarkNode)
    fun handleOpeningBookmark(item: BookmarkNode, mode: BrowsingMode)
    fun handleBookmarkDeletion(nodes: Set<BookmarkNode>, eventType: Event)
    fun handleBackPressed()
    fun handleSigningIn()
}

@SuppressWarnings("TooManyFunctions")
class DefaultBookmarkController(
    private val context: Context,
    private val navController: NavController,
    private val snackbarPresenter: FenixSnackbarPresenter,
    private val deleteBookmarkNodes: (Set<BookmarkNode>, Event) -> Unit,
    private val invokePendingDeletion: () -> Unit
) : BookmarkController {

    private val activity: HomeActivity = context as HomeActivity
    private val resources: Resources = context.resources
    private val services: Services = activity.components.services

    override fun handleBookmarkTapped(item: BookmarkNode) {
        openInNewTab(item.url!!, true, BrowserDirection.FromBookmarks, BrowsingMode.Normal)
    }

    override fun handleBookmarkExpand(folder: BookmarkNode) {
        navigate(BookmarkFragmentDirections.actionBookmarkFragmentSelf(folder.guid))
    }

    override fun handleSelectionModeSwitch() {
        activity.invalidateOptionsMenu()
    }

    override fun handleBookmarkEdit(node: BookmarkNode) {
        navigate(BookmarkFragmentDirections.actionBookmarkFragmentToBookmarkEditFragment(node.guid))
    }

    override fun handleBookmarkSelected(node: BookmarkNode) {
        snackbarPresenter.present(resources.getString(R.string.bookmark_cannot_edit_root))
    }

    override fun handleCopyUrl(item: BookmarkNode) {
        val urlClipData = ClipData.newPlainText(item.url, item.url)
        context.getSystemService<ClipboardManager>()?.primaryClip = urlClipData
        snackbarPresenter.present(resources.getString(R.string.url_copied))
    }

    override fun handleBookmarkSharing(item: BookmarkNode) {
        navigate(
            BookmarkFragmentDirections.actionBookmarkFragmentToShareFragment(
                data = arrayOf(ShareData(url = item.url, title = item.title))
            )
        )
    }

    override fun handleOpeningBookmark(item: BookmarkNode, mode: BrowsingMode) {
        openInNewTab(item.url!!, true, BrowserDirection.FromBookmarks, mode)
    }

    override fun handleBookmarkDeletion(nodes: Set<BookmarkNode>, eventType: Event) {
        deleteBookmarkNodes(nodes, eventType)
    }

    override fun handleBackPressed() {
        invokePendingDeletion.invoke()
        navController.popBackStack()
    }

    override fun handleSigningIn() {
        invokePendingDeletion.invoke()
        services.launchPairingSignIn(context, navController)
    }

    private fun openInNewTab(
        searchTermOrURL: String,
        newTab: Boolean,
        from: BrowserDirection,
        mode: BrowsingMode
    ) {
        invokePendingDeletion.invoke()
        with(activity) {
            browsingModeManager.mode = mode
            openToBrowserAndLoad(searchTermOrURL, newTab, from)
        }
    }

    private fun navigate(directions: NavDirections) {
        invokePendingDeletion.invoke()
        navController.nav(R.id.bookmarkFragment, directions)
    }
}
