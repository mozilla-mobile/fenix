/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.recenttabs

import android.graphics.Bitmap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.lib.state.helpers.AbstractBinding
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifAnyChanged
import org.mozilla.fenix.ext.asRecentTabs
import org.mozilla.fenix.home.HomeFragmentAction
import org.mozilla.fenix.home.HomeFragmentStore

/**
 * View-bound feature that dispatches recent tab changes to the [HomeFragmentStore] when the
 * [BrowserStore] is updated.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecentTabsListFeature(
    private val browserStore: BrowserStore,
    private val homeStore: HomeFragmentStore
) : AbstractBinding<BrowserState>(browserStore) {

    override suspend fun onState(flow: Flow<BrowserState>) {
        lateinit var recentTabs: ArrayList<RecentTab>
        flow
            // Listen for changes regarding the currently selected tab and the in progress media tab
            // and also for changes (close, undo) in normal tabs that could involve these.
            .ifAnyChanged {
                recentTabs = it.asRecentTabs()
                recentTabs.toArray()
            }
            .collect {
                homeStore.dispatch(HomeFragmentAction.RecentTabsChange(recentTabs))
            }
    }
}

sealed class RecentTab {
    /**
     * A tab that was recently viewed
     *
     * @param tabSessionState Recently viewed [TabSessionState]
     */
    data class Tab(val tabSessionState: TabSessionState) : RecentTab()

    /**
     * A search term group that was recently viewed
     *
     * @param searchTerm The search term that was recently viewed
     * @param tabId The id of the tab that was recently viewed
     * @param url The url that was recently viewed
     * @param icon The icon of the search term that was recently viewed
     * @param count The number of tabs in the search term group
     */
    data class SearchGroup(
        val searchTerm: String,
        val tabId: String,
        val url: String,
        val icon: Bitmap?,
        val count: Int
    ) : RecentTab()
}
