/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.browser

import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import mozilla.components.browser.state.state.ContentState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.service.glean.testing.GleanTestRule
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.tabstray.TrayPagerAdapter
import org.mozilla.fenix.utils.Settings
import org.mozilla.fenix.GleanMetrics.TabsTray as TabsTrayMetrics

@RunWith(FenixRobolectricTestRunner::class)
class InactiveTabsControllerTest {

    private val settings: Settings = mockk(relaxed = true)
    private val browserInteractor: BrowserTrayInteractor = mockk(relaxed = true)
    private val appStore = AppStore()

    @get:Rule
    val gleanTestRule = GleanTestRule(testContext)

    @Test
    fun `WHEN the inactive tabs section is expanded THEN the expanded telemetry event should be report`() {
        val controller = InactiveTabsController(appStore, settings, browserInteractor)

        assertFalse(TabsTrayMetrics.inactiveTabsExpanded.testHasValue())
        assertFalse(TabsTrayMetrics.inactiveTabsCollapsed.testHasValue())

        controller.updateCardExpansion(true)

        assertTrue(TabsTrayMetrics.inactiveTabsExpanded.testHasValue())
        assertFalse(TabsTrayMetrics.inactiveTabsCollapsed.testHasValue())
    }

    @Test
    fun `WHEN the inactive tabs section is collapsed THEN the collapsed telemetry event should be report`() {
        val controller = InactiveTabsController(appStore, settings, browserInteractor)

        assertFalse(TabsTrayMetrics.inactiveTabsExpanded.testHasValue())
        assertFalse(TabsTrayMetrics.inactiveTabsCollapsed.testHasValue())

        controller.updateCardExpansion(false)

        assertFalse(TabsTrayMetrics.inactiveTabsExpanded.testHasValue())
        assertTrue(TabsTrayMetrics.inactiveTabsCollapsed.testHasValue())
    }

    @Test
    fun `WHEN the inactive tabs auto-close feature prompt is dismissed THEN update settings and report the telemetry event`() {
        val controller = spyk(InactiveTabsController(appStore, settings, browserInteractor))

        assertFalse(TabsTrayMetrics.autoCloseDimissed.testHasValue())

        controller.close()

        assertTrue(TabsTrayMetrics.autoCloseDimissed.testHasValue())
        verify { settings.hasInactiveTabsAutoCloseDialogBeenDismissed = true }
    }

    @Test
    fun `WHEN the inactive tabs auto-close feature prompt is accepted THEN update settings and report the telemetry event`() {
        val controller = spyk(InactiveTabsController(appStore, settings, browserInteractor))

        assertFalse(TabsTrayMetrics.autoCloseTurnOnClicked.testHasValue())

        controller.enableAutoClosed()

        assertTrue(TabsTrayMetrics.autoCloseTurnOnClicked.testHasValue())
        verify { settings.closeTabsAfterOneMonth = true }
        verify { settings.closeTabsAfterOneWeek = false }
        verify { settings.closeTabsAfterOneDay = false }
        verify { settings.manuallyCloseTabs = false }
        verify { settings.hasInactiveTabsAutoCloseDialogBeenDismissed = true }
    }

    @Test
    fun `WHEN an inactive tab is selected THEN the open the tab and report the telemetry event`() {
        val controller = InactiveTabsController(appStore, settings, browserInteractor)
        val tab = TabSessionState(
            id = "tabId",
            content = ContentState(
                url = "www.mozilla.com",
            )
        )

        assertFalse(TabsTrayMetrics.openInactiveTab.testHasValue())

        controller.openInactiveTab(tab)

        verify { browserInteractor.onTabSelected(tab, TrayPagerAdapter.INACTIVE_TABS_FEATURE_NAME) }

        assertTrue(TabsTrayMetrics.openInactiveTab.testHasValue())
    }

    @Test
    fun `WHEN an inactive tab is closed THEN the close the tab and report the telemetry event`() {
        val controller = InactiveTabsController(appStore, settings, browserInteractor)
        val tab = TabSessionState(
            id = "tabId",
            content = ContentState(
                url = "www.mozilla.com",
            )
        )

        assertFalse(TabsTrayMetrics.openInactiveTab.testHasValue())

        controller.openInactiveTab(tab)

        verify { browserInteractor.onTabSelected(tab, TrayPagerAdapter.INACTIVE_TABS_FEATURE_NAME) }

        assertTrue(TabsTrayMetrics.openInactiveTab.testHasValue())
    }
}
