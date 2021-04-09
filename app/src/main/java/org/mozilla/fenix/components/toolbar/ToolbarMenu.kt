/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.item.BrowserMenuItemToolbar

interface ToolbarMenu {
    sealed class DefaultItem {
        object Settings : DefaultItem()
        data class RequestDesktop(val isChecked: Boolean) : DefaultItem()
        object FindInPage : DefaultItem()
        object Share : DefaultItem()
        data class Back(val viewHistory: Boolean) : DefaultItem()
        data class Forward(val viewHistory: Boolean) : DefaultItem()
        data class Reload(val bypassCache: Boolean) : DefaultItem()
        object Stop : DefaultItem()
        object OpenInFenix : DefaultItem()
        object SaveToCollection : DefaultItem()
        object AddToTopSites : DefaultItem()
        object InstallPwaToHomeScreen : DefaultItem()
        object AddToHomeScreen : DefaultItem()
        object SyncedTabs : DefaultItem()
        object SyncAccount : DefaultItem()
        object AddonsManager : DefaultItem()
        object Quit : DefaultItem()
        object OpenInApp : DefaultItem()
        object SetDefaultBrowser : DefaultItem()
        object Bookmark : DefaultItem()
        object CustomizeReaderView : DefaultItem()
        object Bookmarks : DefaultItem()
        object History : DefaultItem()
        object Downloads : DefaultItem()
        object NewTab : DefaultItem()
    }

    val menuBuilder: BrowserMenuBuilder
    val menuToolbarNavigation: BrowserMenuItemToolbar
}
