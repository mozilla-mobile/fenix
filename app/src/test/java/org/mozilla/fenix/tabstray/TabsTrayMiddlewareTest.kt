/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.tabstray.browser.TabGroup
import org.mozilla.fenix.telemetry.TabsTrayMiddleware
import org.mozilla.fenix.utils.Settings

class TabsTrayMiddlewareTest {

    private lateinit var store: TabsTrayStore
    private lateinit var settings: Settings
    private lateinit var tabsTrayMiddleware: TabsTrayMiddleware
    private lateinit var metrics: MetricController

    @Before
    fun setUp() {
        settings = mockk(relaxed = true)
        metrics = mockk(relaxed = true)
        tabsTrayMiddleware = TabsTrayMiddleware(
            settings,
            metrics
        )
        store = TabsTrayStore(
            middlewares = listOf(tabsTrayMiddleware),
            initialState = TabsTrayState()
        )
    }

    @Test
    fun `WHEN metrics are reported AND the inactive tabs is enabled THEN report the count of inactive tabs`() {
        every { settings.inactiveTabsAreEnabled } returns true
        store.dispatch(TabsTrayAction.ReportTabMetrics(10, emptyList()))
        verify { metrics.track(Event.TabsTrayHasInactiveTabs(10)) }
    }

    @Test
    fun `WHEN metrics are reported AND there are search term tab groups THEN report the average tabs per group`() {
        store.dispatch(TabsTrayAction.ReportTabMetrics(0, generateSearchTermTabGroupsForAverage()))
        verify { metrics.track(Event.AverageTabsPerSearchTermGroup(5.0)) }
    }

    @Test
    fun `WHEN metrics are reported AND there are search term tab groups THEN report the distribution of tab sizes`() {
        store.dispatch(TabsTrayAction.ReportTabMetrics(0, generateSearchTermTabGroupsForDistribution()))
        verify { metrics.track(Event.SearchTermGroupSizeDistribution(correctDistributionReportingValues)) }
    }

    @Test
    fun `WHEN metrics are reported THEN report the count of search term tab groups`() {
        store.dispatch(TabsTrayAction.ReportTabMetrics(0, emptyList()))
        verify { metrics.track(Event.SearchTermGroupCount(0)) }
    }

    @Test
    fun testGenerateTabGroupSizeMappedValue() {
        assertEquals(1L, tabsTrayMiddleware.generateTabGroupSizeMappedValue(2))
        assertEquals(2L, tabsTrayMiddleware.generateTabGroupSizeMappedValue(3))
        assertEquals(2L, tabsTrayMiddleware.generateTabGroupSizeMappedValue(4))
        assertEquals(2L, tabsTrayMiddleware.generateTabGroupSizeMappedValue(5))
        assertEquals(3L, tabsTrayMiddleware.generateTabGroupSizeMappedValue(6))
        assertEquals(3L, tabsTrayMiddleware.generateTabGroupSizeMappedValue(7))
        assertEquals(3L, tabsTrayMiddleware.generateTabGroupSizeMappedValue(8))
        assertEquals(3L, tabsTrayMiddleware.generateTabGroupSizeMappedValue(9))
        assertEquals(3L, tabsTrayMiddleware.generateTabGroupSizeMappedValue(10))
        assertEquals(4L, tabsTrayMiddleware.generateTabGroupSizeMappedValue(11))
        assertEquals(4L, tabsTrayMiddleware.generateTabGroupSizeMappedValue(12))
        assertEquals(4L, tabsTrayMiddleware.generateTabGroupSizeMappedValue(20))
        assertEquals(4L, tabsTrayMiddleware.generateTabGroupSizeMappedValue(50))
    }

    private fun generateSearchTermTabGroupsForAverage(): List<TabGroup> {
        val group1 = TabGroup("", mockk(relaxed = true), 0L)
        val group2 = TabGroup("", mockk(relaxed = true), 0L)
        val group3 = TabGroup("", mockk(relaxed = true), 0L)

        every { group1.tabs.size } returns 8
        every { group2.tabs.size } returns 4
        every { group3.tabs.size } returns 3

        return listOf(group1, group2, group3)
    }

    private fun generateSearchTermTabGroupsForDistribution(): List<TabGroup> {
        val group1 = TabGroup("", mockk(relaxed = true), 0L)
        val group2 = TabGroup("", mockk(relaxed = true), 0L)
        val group3 = TabGroup("", mockk(relaxed = true), 0L)
        val group4 = TabGroup("", mockk(relaxed = true), 0L)

        every { group1.tabs.size } returns 8
        every { group2.tabs.size } returns 4
        every { group3.tabs.size } returns 2
        every { group4.tabs.size } returns 12

        return listOf(group1, group2, group3, group4)
    }

    private val correctDistributionReportingValues = listOf(3L, 2L, 1L, 4L)
}
