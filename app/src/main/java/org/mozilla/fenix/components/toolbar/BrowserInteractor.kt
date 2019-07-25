/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import android.content.Context
import android.content.Intent
import androidx.navigation.NavController
import mozilla.components.browser.session.Session
import mozilla.components.feature.app.links.AppLinksUseCases
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.BrowserFragmentDirections
import org.mozilla.fenix.browser.readermode.ReaderModeController
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.metrics
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.quickactionsheet.QuickActionSheetViewInteractor
import org.mozilla.fenix.utils.ItsNotBrokenSnack

class BrowserInteractor(
    private val context: Context,
    private val navController: NavController,
    private val store: BrowserStore,
    private val browserToolbarController: BrowserToolbarController,
    private val readerModeController: ReaderModeController,
    private val bookmarkTapped: (Session) -> Unit,
    private val appLinksUseCases: AppLinksUseCases,
    private val currentSession: Session
) : BrowserToolbarViewInteractor, QuickActionSheetViewInteractor {

    override fun onBrowserToolbarClicked() {
        browserToolbarController.handleToolbarClick()
    }

    override fun onBrowserToolbarMenuItemTapped(item: ToolbarMenu.Item) {
        browserToolbarController.handleToolbarItemInteraction(item)
    }

    override fun onQuickActionSheetOpened() {
        context.metrics.track(Event.QuickActionSheetOpened)
    }

    override fun onQuickActionSheetClosed() {
        context.metrics.track(Event.QuickActionSheetClosed)
    }

    override fun onQuickActionSheetSharePressed() {
        context.metrics.track(Event.QuickActionSheetShareTapped)
        currentSession.url.let {
            val directions = BrowserFragmentDirections.actionBrowserFragmentToShareFragment(it)
            navController.nav(R.id.browserFragment, directions)
        }
    }

    override fun onQuickActionSheetDownloadPressed() {
        context.metrics.track(Event.QuickActionSheetDownloadTapped)
        ItsNotBrokenSnack(context).showSnackbar(issueNumber = "348")
    }

    override fun onQuickActionSheetBookmarkPressed() {
        context.metrics.track(Event.QuickActionSheetBookmarkTapped)
        currentSession.let(bookmarkTapped)
    }

    override fun onQuickActionSheetReadPressed() {
        context.metrics.track(Event.QuickActionSheetReadTapped)
        val enabled = currentSession.readerMode
        if (enabled) {
            readerModeController.hideReaderView()
        } else {
            readerModeController.showReaderView()
        }
        store.dispatch(QuickActionSheetAction.ReaderActiveStateChange(!enabled))
    }

    override fun onQuickActionSheetOpenLinkPressed() {
        val getRedirect = appLinksUseCases.appLinkRedirect
        val redirect = currentSession.let {
            getRedirect.invoke(it.url)
        }

        redirect.appIntent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        appLinksUseCases.openAppLink.invoke(redirect)
    }

    override fun onQuickActionSheetAppearancePressed() {
        // TODO telemetry: https://github.com/mozilla-mobile/fenix/issues/2267
        readerModeController.showControls()
    }
}