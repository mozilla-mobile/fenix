/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.browser

import io.mockk.every
import io.mockk.mockk
import mozilla.components.browser.state.state.TabGroup
import mozilla.components.browser.state.state.TabPartition
import mozilla.components.browser.state.state.createTab
import mozilla.components.support.test.libstate.ext.waitUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mozilla.fenix.tabstray.SEARCH_TERM_TAB_GROUPS
import org.mozilla.fenix.tabstray.TabsTrayStore
import org.mozilla.fenix.utils.Settings

class TabSorterTest {
    private val settings: Settings = mockk()
    private var inactiveTimestamp = 0L
    private val tabsTrayStore = TabsTrayStore()

    @Before
    fun setUp() {
        every { settings.inactiveTabsAreEnabled }.answers { true }
        every { settings.searchTermTabGroupsAreEnabled }.answers { true }
    }

    @Test
    fun `WHEN updated with one normal tab THEN adapter have only one normal tab and no header`() {
        val tabSorter = TabSorter(settings, tabsTrayStore)

        tabSorter.updateTabs(
            listOf(
                createTab(url = "url", id = "tab1", lastAccess = System.currentTimeMillis())
            ),
            null,
            selectedTabId = "tab1"
        )

        tabsTrayStore.waitUntilIdle()

        assertEquals(tabsTrayStore.state.inactiveTabs.size, 0)
        assertEquals(tabsTrayStore.state.searchTermPartition?.tabGroups?.size, null)
        assertEquals(tabsTrayStore.state.normalTabs.size, 1)
    }

    @Test
    fun `WHEN updated with one normal tab and two search term tab THEN adapter have normal tab and a search group`() {
        val tabSorter = TabSorter(settings, tabsTrayStore)
        val searchTab1 = createTab(url = "url", id = "tab2", lastAccess = System.currentTimeMillis(), searchTerms = "mozilla")
        val searchTab2 = createTab(url = "url", id = "tab3", lastAccess = System.currentTimeMillis(), searchTerms = "mozilla")

        tabSorter.updateTabs(
            listOf(
                createTab(url = "url", id = "tab1", lastAccess = System.currentTimeMillis()),
                searchTab1, searchTab2
            ),
            TabPartition(SEARCH_TERM_TAB_GROUPS, listOf(TabGroup("mozilla", "", listOf(searchTab1.id, searchTab2.id)))),
            selectedTabId = "tab1"
        )

        tabsTrayStore.waitUntilIdle()

        assertEquals(tabsTrayStore.state.inactiveTabs.size, 0)
        assertEquals(tabsTrayStore.state.searchTermPartition?.tabGroups?.size, 1)
        assertEquals(tabsTrayStore.state.normalTabs.size, 1)
    }

    @Test
    fun `WHEN updated with one normal tab, one inactive tab and two search term tab THEN adapter have normal tab, inactive tab and a search group`() {
        val tabSorter = TabSorter(settings, tabsTrayStore)
        val searchTab1 = createTab(url = "url", id = "tab3", lastAccess = System.currentTimeMillis(), searchTerms = "mozilla")
        val searchTab2 = createTab(url = "url", id = "tab4", lastAccess = System.currentTimeMillis(), searchTerms = "mozilla")

        tabSorter.updateTabs(
            listOf(
                createTab(url = "url", id = "tab1", lastAccess = System.currentTimeMillis()),
                createTab(url = "url", id = "tab2", lastAccess = inactiveTimestamp, createdAt = inactiveTimestamp),
                searchTab1, searchTab2
            ),
            TabPartition(SEARCH_TERM_TAB_GROUPS, listOf(TabGroup("mozilla", "", listOf(searchTab1.id, searchTab2.id)))),
            selectedTabId = "tab1"
        )

        tabsTrayStore.waitUntilIdle()

        assertEquals(tabsTrayStore.state.inactiveTabs.size, 1)
        assertEquals(tabsTrayStore.state.searchTermPartition?.tabGroups?.size, 1)
        assertEquals(tabsTrayStore.state.normalTabs.size, 1)
    }

    @Test
    fun `WHEN inactive tabs is off THEN adapter have no inactive tab`() {
        every { settings.inactiveTabsAreEnabled }.answers { false }
        val tabSorter = TabSorter(settings, tabsTrayStore)

        tabSorter.updateTabs(
            listOf(
                createTab(url = "url", id = "tab1", lastAccess = System.currentTimeMillis()),
                createTab(
                    url = "url",
                    id = "tab2",
                    lastAccess = inactiveTimestamp,
                    createdAt = inactiveTimestamp
                ),
                createTab(
                    url = "url",
                    id = "tab3",
                    lastAccess = System.currentTimeMillis(),
                    searchTerms = "mozilla"
                ),
                createTab(
                    url = "url",
                    id = "tab4",
                    lastAccess = System.currentTimeMillis(),
                    searchTerms = "mozilla"
                )
            ),
            TabPartition(SEARCH_TERM_TAB_GROUPS, listOf(TabGroup("mozilla", "", listOf("tab3", "tab4")))),
            selectedTabId = "tab1"
        )

        tabsTrayStore.waitUntilIdle()

        assertEquals(tabsTrayStore.state.inactiveTabs.size, 0)
        assertEquals(tabsTrayStore.state.searchTermPartition?.tabGroups?.size, 1)
        assertEquals(tabsTrayStore.state.normalTabs.size, 2)
    }

    @Test
    fun `WHEN search term tabs is off THEN adapter have no search term group`() {
        every { settings.searchTermTabGroupsAreEnabled }.answers { false }
        val tabSorter = TabSorter(settings, tabsTrayStore)

        tabSorter.updateTabs(
            listOf(
                createTab(url = "url", id = "tab1", lastAccess = System.currentTimeMillis()),
                createTab(
                    url = "url",
                    id = "tab2",
                    lastAccess = inactiveTimestamp,
                    createdAt = inactiveTimestamp
                ),
                createTab(
                    url = "url",
                    id = "tab3",
                    lastAccess = System.currentTimeMillis(),
                    searchTerms = "mozilla"
                ),
                createTab(
                    url = "url",
                    id = "tab4",
                    lastAccess = System.currentTimeMillis(),
                    searchTerms = "mozilla"
                )
            ),
            TabPartition(SEARCH_TERM_TAB_GROUPS, listOf(TabGroup("mozilla", "", listOf("tab3", "tab4")))),
            selectedTabId = "tab1"
        )

        tabsTrayStore.waitUntilIdle()

        assertEquals(tabsTrayStore.state.inactiveTabs.size, 1)
        assertEquals(tabsTrayStore.state.searchTermPartition?.tabGroups?.size, 0)
        assertEquals(tabsTrayStore.state.normalTabs.size, 3)
    }

    @Test
    fun `WHEN both inactive tabs and search term tabs are off THEN adapter have only normal tabs`() {
        every { settings.inactiveTabsAreEnabled }.answers { false }
        every { settings.searchTermTabGroupsAreEnabled }.answers { false }
        val tabSorter = TabSorter(settings, tabsTrayStore)

        tabSorter.updateTabs(
            listOf(
                createTab(url = "url", id = "tab1", lastAccess = System.currentTimeMillis()),
                createTab(
                    url = "url",
                    id = "tab2",
                    lastAccess = inactiveTimestamp
                ),
                createTab(
                    url = "url",
                    id = "tab3",
                    lastAccess = System.currentTimeMillis(),
                    searchTerms = "mozilla"
                ),
                createTab(
                    url = "url",
                    id = "tab4",
                    lastAccess = System.currentTimeMillis(),
                    searchTerms = "mozilla"
                )
            ),
            TabPartition(SEARCH_TERM_TAB_GROUPS, listOf(TabGroup("mozilla", "", mockk()), TabGroup("mozilla", "", mockk()))),
            selectedTabId = "tab1"
        )

        tabsTrayStore.waitUntilIdle()

        assertEquals(tabsTrayStore.state.inactiveTabs.size, 0)
        assertEquals(tabsTrayStore.state.searchTermPartition?.tabGroups?.size, 0)
        assertEquals(tabsTrayStore.state.normalTabs.size, 4)
    }

    @Test
    fun `WHEN only one search term tab THEN there is no search group`() {
        val tabSorter = TabSorter(settings, tabsTrayStore)
        val tab1 =
            createTab(
                url = "url", id = "tab1", lastAccess = System.currentTimeMillis(),
                searchTerms = "mozilla"
            )

        tabSorter.updateTabs(
            listOf(tab1),
            TabPartition(SEARCH_TERM_TAB_GROUPS, listOf(TabGroup("mozilla", "", listOf(tab1.id)))),
            selectedTabId = "tab1"
        )

        tabsTrayStore.waitUntilIdle()

        assertEquals(tabsTrayStore.state.inactiveTabs.size, 0)
        assertEquals(tabsTrayStore.state.searchTermPartition?.tabGroups?.size, 0)
        assertEquals(tabsTrayStore.state.normalTabs.size, 1)
    }

    @Test
    fun `WHEN remove second last one search term tab THEN search group is kept even if there's only one tab`() {
        val tabSorter = TabSorter(settings, tabsTrayStore)
        val tab1 = createTab(url = "url", id = "tab1", lastAccess = System.currentTimeMillis(), searchTerms = "mozilla")
        val tab2 = createTab(url = "url", id = "tab2", lastAccess = System.currentTimeMillis(), searchTerms = "mozilla")

        tabSorter.updateTabs(
            listOf(tab1, tab2),
            TabPartition(SEARCH_TERM_TAB_GROUPS, listOf(TabGroup("mozilla", "", listOf(tab1.id, tab2.id)))),
            selectedTabId = "tab1"
        )

        tabsTrayStore.waitUntilIdle()

        assertEquals(tabsTrayStore.state.inactiveTabs.size, 0)
        assertEquals(tabsTrayStore.state.searchTermPartition?.tabGroups?.size, 1)
        assertEquals(tabsTrayStore.state.normalTabs.size, 0)

        tabSorter.updateTabs(
            listOf(tab1),
            TabPartition(SEARCH_TERM_TAB_GROUPS, listOf(TabGroup("mozilla", "", listOf(tab1.id)))),
            selectedTabId = "tab1"
        )

        tabsTrayStore.waitUntilIdle()

        assertEquals(tabsTrayStore.state.inactiveTabs.size, 0)
        assertEquals(tabsTrayStore.state.searchTermPartition?.tabGroups?.size, 1)
        assertEquals(tabsTrayStore.state.normalTabs.size, 0)
    }
}
