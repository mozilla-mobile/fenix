/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.browser

import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.concept.tabstray.Tab

data class TabGroup(
    /**
     * A title for the tab group.
     */
    val searchTerm: String,

    /**
     * The list of tabs belonging to this tab group.
     */
    val tabs: List<Tab>? = null,

    /**
     * The list of tabSessionStates belonging to this tab group.
     */
    val tabSessionStates: List<TabSessionState>? = null,

    /**
     * The last time tabs in this group was accessed.
     */
    val lastAccess: Long
)
