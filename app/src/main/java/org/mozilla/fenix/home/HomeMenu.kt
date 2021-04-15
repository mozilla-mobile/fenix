/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home

interface HomeMenu {
    /**
     * Items used in the home screen menu.
     */
    sealed class Item {
        object Bookmarks : Item()
        object History : Item()
        object Downloads : Item()
        object Extensions : Item()
        object SyncedTabs : Item()
        object SyncAccount : Item()
        object WhatsNew : Item()
        object Help : Item()
        object Settings : Item()
        object Quit : Item()
        object ReconnectSync : Item()
        data class DesktopMode(val checked: Boolean) : Item()
    }
}
