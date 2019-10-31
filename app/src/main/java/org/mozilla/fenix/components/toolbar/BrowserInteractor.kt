/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import android.content.Context
import mozilla.components.browser.session.Session
import org.mozilla.fenix.browser.readermode.ReaderModeController

open class BrowserToolbarInteractor(
    private val browserToolbarController: BrowserToolbarController
) : BrowserToolbarViewInteractor {

    override fun onTabCounterClicked() {
        browserToolbarController.handleTabCounterClick()
    }

    override fun onBrowserToolbarPaste(text: String) {
        browserToolbarController.handleToolbarPaste(text)
    }

    override fun onBrowserToolbarPasteAndGo(text: String) {
        browserToolbarController.handleToolbarPasteAndGo(text)
    }

    override fun onBrowserToolbarClicked() {
        browserToolbarController.handleToolbarClick()
    }

    override fun onBrowserToolbarMenuItemTapped(item: ToolbarMenu.Item) {
        browserToolbarController.handleToolbarItemInteraction(item)
    }
}

class BrowserInteractor( // TODO remove
    private val context: Context,
    private val store: BrowserFragmentStore,
    browserToolbarController: BrowserToolbarController,
    private val readerModeController: ReaderModeController,
    private val currentSession: Session?
) : BrowserToolbarInteractor(browserToolbarController)
