/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import mozilla.components.browser.state.selector.normalTabs
import mozilla.components.browser.state.selector.selectedNormalTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.feature.tabs.ext.hasMediaPlayed
import org.mozilla.fenix.FeatureFlags
import org.mozilla.fenix.home.recenttabs.RecentTab
import org.mozilla.fenix.tabstray.browser.TabGroup
import org.mozilla.fenix.tabstray.browser.maxActiveTime
import org.mozilla.fenix.tabstray.ext.isNormalTabActiveWithSearchTerm
import kotlin.math.max

/**
 * Get the last opened normal tab and the last tab with in progress media, if available.
 *
 * @return A array list of the last opened tab and the last tab with in progress media
 * if distinct and available or an empty list.
 */
fun BrowserState.asRecentTabs(): ArrayList<RecentTab> {
    return ArrayList<RecentTab>().apply {
        val lastOpenedNormalTab = lastOpenedNormalTab
        val inProgressMediaTab = inProgressMediaTab

        lastOpenedNormalTab?.let { add(RecentTab.Tab(it)) }

        if (inProgressMediaTab == lastOpenedNormalTab) {
            secondToLastOpenedNormalTab?.let { add(RecentTab.Tab(it)) }
        } else {
            inProgressMediaTab?.let { add(RecentTab.Tab(it)) }
        }

        if (FeatureFlags.tabGroupFeature) {
            lastSearchGroup()?.let {
                add(it)
            }
        }
    }
}

/**
 *  Get the selected normal tab or the last accessed normal tab
 *  if there is no selected tab or the selected tab is a private one.
 */
val BrowserState.lastOpenedNormalTab: TabSessionState?
    get() = selectedNormalTab ?: normalTabs.maxByOrNull { it.lastAccess }

/**
 *  Get the second-to-last accessed normal tab.
 */
val BrowserState.secondToLastOpenedNormalTab: TabSessionState?
    get() = when {
        normalTabs.size <= 1 -> null
        else -> normalTabs.sortedByDescending { it.lastAccess }[1]
    }

/**
 * Get the last tab with in progress media.
 */
val BrowserState.inProgressMediaTab: TabSessionState?
    get() = normalTabs
        .filter { it.hasMediaPlayed() }
        .maxByOrNull { it.lastMediaAccessState.lastMediaAccess }

/**
 * Get the last search term.
 */
val BrowserState.lastActiveSearchTermTab: TabSessionState?
    get() = normalTabs
        .filter {
            it.isNormalTabActiveWithSearchTerm(maxActiveTime)
        }
        .maxByOrNull { it.lastAccess }

/**
 * Get search term groups sorted by last access time.
 */
fun List<TabSessionState>.toSearchGroup(): List<TabGroup> {
    val data = filter {
        it.isNormalTabActiveWithSearchTerm(maxActiveTime)
    }.groupBy {
        if (it.content.searchTerms.isBlank()) {
            it.historyMetadata?.searchTerm ?: ""
        } else {
            it.content.searchTerms
        }.lowercase()
    }

    return data.map { mapEntry ->
        val searchTerm = mapEntry.key.replaceFirstChar(Char::uppercase)
        val groupTabs = mapEntry.value
        val groupMax = groupTabs.fold(0L) { acc, tab ->
            max(tab.lastAccess, acc)
        }

        TabGroup(
            searchTerm = searchTerm,
            tabSessionStates = groupTabs,
            lastAccess = groupMax
        )
    }.sortedBy { it.lastAccess }
}

/**
 * Get the most recent search term group.
 */
fun BrowserState.lastSearchGroup(): RecentTab.SearchGroup? {
    val tabGroup = normalTabs.toSearchGroup().lastOrNull()
    if (tabGroup?.tabSessionStates == null) {
        return null
    }

    return RecentTab.SearchGroup(
        tabGroup.searchTerm,
        tabGroup.tabSessionStates.first().id,
        tabGroup.tabSessionStates.first().content.url,
        tabGroup.tabSessionStates.first().content.icon,
        tabGroup.tabSessionStates.count()
    )
}
