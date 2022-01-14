/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.browser

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.tabstray.TabsTray
import mozilla.components.lib.state.helpers.AbstractBinding
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged
import org.mozilla.fenix.tabstray.TabsTrayState
import org.mozilla.fenix.tabstray.TabsTrayStore

/**
 * A normal tabs observer that updates the provided [TabsTray].
 */
class NormalTabsBinding(
    store: TabsTrayStore,
    private val browserStore: BrowserStore,
    private val tabsTray: TabsTray
) : AbstractBinding<TabsTrayState>(store) {
    override suspend fun onState(flow: Flow<TabsTrayState>) {
        flow.ifChanged { Pair(it.normalTabs, it.searchTermPartition) }
            .collect {
                // Getting the selectedTabId from the BrowserStore at a different time might lead to a race.
                tabsTray.updateTabs(it.normalTabs, it.searchTermPartition, browserStore.state.selectedTabId)
            }
    }
}
