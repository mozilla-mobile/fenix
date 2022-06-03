/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.telemetry

import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.engine.EngineMiddleware
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.state.recover.RecoverableTab
import mozilla.components.browser.state.state.recover.TabState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.service.glean.testing.GleanTestRule
import mozilla.components.support.base.android.Clock
import mozilla.components.support.test.ext.joinBlocking
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.rule.MainCoroutineRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.GleanMetrics.Events
import org.mozilla.fenix.GleanMetrics.Metrics
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.utils.Settings
import org.mozilla.fenix.GleanMetrics.EngineTab as EngineMetrics

@RunWith(FenixRobolectricTestRunner::class)
class TelemetryMiddlewareTest {

    private lateinit var store: BrowserStore
    private lateinit var settings: Settings
    private lateinit var telemetryMiddleware: TelemetryMiddleware

    @get:Rule
    val coroutinesTestRule = MainCoroutineRule()

    @get:Rule
    val gleanRule = GleanTestRule(ApplicationProvider.getApplicationContext())

    private val clock = FakeClock()

    @Before
    fun setUp() {
        Clock.delegate = clock

        settings = Settings(testContext)
        telemetryMiddleware = TelemetryMiddleware(settings,)
        store = BrowserStore(
            middleware = listOf(telemetryMiddleware) + EngineMiddleware.create(engine = mockk()),
            initialState = BrowserState()
        )
    }

    @After
    fun tearDown() {
        Clock.reset()
    }

    @Test
    fun `WHEN a tab is added THEN the open tab count is updated`() {
        assertEquals(0, settings.openTabsCount)
        assertFalse(Metrics.hasOpenTabs.testHasValue())

        store.dispatch(TabListAction.AddTabAction(createTab("https://mozilla.org"))).joinBlocking()
        assertEquals(1, settings.openTabsCount)

        assertTrue(Metrics.hasOpenTabs.testHasValue())
        assertTrue(Metrics.hasOpenTabs.testGetValue())
    }

    @Test
    fun `WHEN a private tab is added THEN the open tab count is not updated`() {
        assertEquals(0, settings.openTabsCount)
        assertFalse(Metrics.hasOpenTabs.testHasValue())

        store.dispatch(TabListAction.AddTabAction(createTab("https://mozilla.org", private = true))).joinBlocking()
        assertEquals(0, settings.openTabsCount)

        assertTrue(Metrics.hasOpenTabs.testHasValue())
        assertFalse(Metrics.hasOpenTabs.testGetValue())
    }

    @Test
    fun `WHEN multiple tabs are added THEN the open tab count is updated`() {
        assertEquals(0, settings.openTabsCount)
        assertFalse(Metrics.hasOpenTabs.testHasValue())

        store.dispatch(
            TabListAction.AddMultipleTabsAction(
                listOf(
                    createTab("https://mozilla.org"),
                    createTab("https://firefox.com")
                )
            )
        ).joinBlocking()

        assertEquals(2, settings.openTabsCount)

        assertTrue(Metrics.hasOpenTabs.testHasValue())
        assertTrue(Metrics.hasOpenTabs.testGetValue())
    }

    @Test
    fun `WHEN a tab is removed THEN the open tab count is updated`() {
        assertFalse(Metrics.hasOpenTabs.testHasValue())

        store.dispatch(
            TabListAction.AddMultipleTabsAction(
                listOf(
                    createTab(id = "1", url = "https://mozilla.org"),
                    createTab(id = "2", url = "https://firefox.com")
                )
            )
        ).joinBlocking()
        assertEquals(2, settings.openTabsCount)

        store.dispatch(TabListAction.RemoveTabAction("1")).joinBlocking()
        assertEquals(1, settings.openTabsCount)

        assertTrue(Metrics.hasOpenTabs.testHasValue())
        assertTrue(Metrics.hasOpenTabs.testGetValue())
    }

    @Test
    fun `WHEN all tabs are removed THEN the open tab count is updated`() {
        assertFalse(Metrics.hasOpenTabs.testHasValue())

        store.dispatch(
            TabListAction.AddMultipleTabsAction(
                listOf(
                    createTab("https://mozilla.org"),
                    createTab("https://firefox.com")
                )
            )
        ).joinBlocking()
        assertEquals(2, settings.openTabsCount)

        assertTrue(Metrics.hasOpenTabs.testHasValue())
        assertTrue(Metrics.hasOpenTabs.testGetValue())

        store.dispatch(TabListAction.RemoveAllTabsAction()).joinBlocking()
        assertEquals(0, settings.openTabsCount)

        assertTrue(Metrics.hasOpenTabs.testHasValue())
        assertFalse(Metrics.hasOpenTabs.testGetValue())
    }

    @Test
    fun `WHEN all normal tabs are removed THEN the open tab count is updated`() {
        assertFalse(Metrics.hasOpenTabs.testHasValue())

        store.dispatch(
            TabListAction.AddMultipleTabsAction(
                listOf(
                    createTab("https://mozilla.org"),
                    createTab("https://firefox.com"),
                    createTab("https://getpocket.com", private = true)
                )
            )
        ).joinBlocking()
        assertEquals(2, settings.openTabsCount)
        assertTrue(Metrics.hasOpenTabs.testHasValue())
        assertTrue(Metrics.hasOpenTabs.testGetValue())

        store.dispatch(TabListAction.RemoveAllNormalTabsAction).joinBlocking()
        assertEquals(0, settings.openTabsCount)
        assertTrue(Metrics.hasOpenTabs.testHasValue())
        assertFalse(Metrics.hasOpenTabs.testGetValue())
    }

    @Test
    fun `WHEN tabs are restored THEN the open tab count is updated`() {
        assertEquals(0, settings.openTabsCount)
        assertFalse(Metrics.hasOpenTabs.testHasValue())

        val tabsToRestore = listOf(
            RecoverableTab(null, TabState(url = "https://mozilla.org", id = "1")),
            RecoverableTab(null, TabState(url = "https://firefox.com", id = "2"))
        )

        store.dispatch(
            TabListAction.RestoreAction(
                tabs = tabsToRestore,
                restoreLocation = TabListAction.RestoreAction.RestoreLocation.BEGINNING
            )
        ).joinBlocking()
        assertEquals(2, settings.openTabsCount)

        assertTrue(Metrics.hasOpenTabs.testHasValue())
        assertTrue(Metrics.hasOpenTabs.testGetValue())
    }

    @Test
    fun `GIVEN a normal page is loading WHEN loading is complete THEN we record a UriOpened event`() {
        val tab = createTab(id = "1", url = "https://mozilla.org")
        assertFalse(Events.normalAndPrivateUriCount.testHasValue())

        store.dispatch(TabListAction.AddTabAction(tab)).joinBlocking()
        store.dispatch(ContentAction.UpdateLoadingStateAction(tab.id, true)).joinBlocking()
        assertFalse(Events.normalAndPrivateUriCount.testHasValue())

        store.dispatch(ContentAction.UpdateLoadingStateAction(tab.id, false)).joinBlocking()
        assertTrue(Events.normalAndPrivateUriCount.testHasValue())
        val count = Events.normalAndPrivateUriCount.testGetValue()
        assertEquals(1, count)
    }

    @Test
    fun `GIVEN a private page is loading WHEN loading is complete THEN we record a UriOpened event`() {
        val tab = createTab(id = "1", url = "https://mozilla.org", private = true)
        assertFalse(Events.normalAndPrivateUriCount.testHasValue())

        store.dispatch(TabListAction.AddTabAction(tab)).joinBlocking()
        store.dispatch(ContentAction.UpdateLoadingStateAction(tab.id, true)).joinBlocking()
        assertFalse(Events.normalAndPrivateUriCount.testHasValue())

        store.dispatch(ContentAction.UpdateLoadingStateAction(tab.id, false)).joinBlocking()
        val count = Events.normalAndPrivateUriCount.testGetValue()
        assertEquals(1, count)
    }

    @Test
    fun `WHEN foreground tab getting killed THEN middleware counts it`() {
        store.dispatch(
            TabListAction.RestoreAction(
                listOf(
                    RecoverableTab(null, TabState(url = "https://www.mozilla.org", id = "foreground")),
                    RecoverableTab(null, TabState(url = "https://getpocket.com", id = "background_pocket")),
                    RecoverableTab(null, TabState(url = "https://theverge.com", id = "background_verge"))
                ),
                selectedTabId = "foreground",
                restoreLocation = TabListAction.RestoreAction.RestoreLocation.BEGINNING
            )
        ).joinBlocking()

        assertFalse(EngineMetrics.kills["foreground"].testHasValue())
        assertFalse(EngineMetrics.kills["background"].testHasValue())

        store.dispatch(
            EngineAction.KillEngineSessionAction("foreground")
        ).joinBlocking()

        assertTrue(EngineMetrics.kills["foreground"].testHasValue())
    }

    @Test
    fun `WHEN background tabs getting killed THEN middleware counts it`() {
        store.dispatch(
            TabListAction.RestoreAction(
                listOf(
                    RecoverableTab(null, TabState(url = "https://www.mozilla.org", id = "foreground")),
                    RecoverableTab(null, TabState(url = "https://getpocket.com", id = "background_pocket")),
                    RecoverableTab(null, TabState(url = "https://theverge.com", id = "background_verge"))
                ),
                selectedTabId = "foreground",
                restoreLocation = TabListAction.RestoreAction.RestoreLocation.BEGINNING
            )
        ).joinBlocking()

        assertFalse(EngineMetrics.kills["foreground"].testHasValue())
        assertFalse(EngineMetrics.kills["background"].testHasValue())

        store.dispatch(
            EngineAction.KillEngineSessionAction("background_pocket")
        ).joinBlocking()

        assertFalse(EngineMetrics.kills["foreground"].testHasValue())
        assertTrue(EngineMetrics.kills["background"].testHasValue())
        assertEquals(1, EngineMetrics.kills["background"].testGetValue())

        store.dispatch(
            EngineAction.KillEngineSessionAction("background_verge")
        ).joinBlocking()

        assertFalse(EngineMetrics.kills["foreground"].testHasValue())
        assertTrue(EngineMetrics.kills["background"].testHasValue())
        assertEquals(2, EngineMetrics.kills["background"].testGetValue())
    }

    @Test
    fun `WHEN foreground tab gets killed THEN middleware records foreground age`() {
        store.dispatch(
            TabListAction.RestoreAction(
                listOf(
                    RecoverableTab(null, TabState(url = "https://www.mozilla.org", id = "foreground")),
                    RecoverableTab(null, TabState(url = "https://getpocket.com", id = "background_pocket")),
                    RecoverableTab(null, TabState(url = "https://theverge.com", id = "background_verge"))
                ),
                selectedTabId = "foreground",
                restoreLocation = TabListAction.RestoreAction.RestoreLocation.BEGINNING
            )
        ).joinBlocking()

        clock.elapsedTime = 100

        store.dispatch(
            EngineAction.LinkEngineSessionAction(
                tabId = "foreground",
                engineSession = mockk(relaxed = true)
            )
        ).joinBlocking()

        assertFalse(EngineMetrics.killForegroundAge.testHasValue())
        assertFalse(EngineMetrics.killBackgroundAge.testHasValue())

        clock.elapsedTime = 500

        store.dispatch(
            EngineAction.KillEngineSessionAction("foreground")
        ).joinBlocking()

        assertTrue(EngineMetrics.killForegroundAge.testHasValue())
        assertFalse(EngineMetrics.killBackgroundAge.testHasValue())
        assertEquals(400_000_000, EngineMetrics.killForegroundAge.testGetValue().sum)
    }

    @Test
    fun `WHEN background tab gets killed THEN middleware records background age`() {
        store.dispatch(
            TabListAction.RestoreAction(
                listOf(
                    RecoverableTab(null, TabState(url = "https://www.mozilla.org", id = "foreground")),
                    RecoverableTab(null, TabState(url = "https://getpocket.com", id = "background_pocket")),
                    RecoverableTab(null, TabState(url = "https://theverge.com", id = "background_verge"))
                ),
                selectedTabId = "foreground",
                restoreLocation = TabListAction.RestoreAction.RestoreLocation.BEGINNING
            )
        ).joinBlocking()

        clock.elapsedTime = 100

        store.dispatch(
            EngineAction.LinkEngineSessionAction(
                tabId = "background_pocket",
                engineSession = mockk(relaxed = true)
            )
        ).joinBlocking()

        clock.elapsedTime = 700

        assertFalse(EngineMetrics.killForegroundAge.testHasValue())
        assertFalse(EngineMetrics.killBackgroundAge.testHasValue())

        store.dispatch(
            EngineAction.KillEngineSessionAction("background_pocket")
        ).joinBlocking()

        assertTrue(EngineMetrics.killBackgroundAge.testHasValue())
        assertFalse(EngineMetrics.killForegroundAge.testHasValue())
        assertEquals(600_000_000, EngineMetrics.killBackgroundAge.testGetValue().sum)
    }
}

internal class FakeClock : Clock.Delegate {
    var elapsedTime: Long = 0
    override fun elapsedRealtime(): Long = elapsedTime
}
