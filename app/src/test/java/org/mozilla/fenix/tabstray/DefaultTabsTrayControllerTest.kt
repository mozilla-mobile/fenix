/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray

import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.selector.getNormalOrPrivateTabs
import mozilla.components.browser.state.state.ContentState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.storage.sync.TabEntry
import mozilla.components.concept.base.profiler.Profiler
import mozilla.components.concept.engine.mediasession.MediaSession
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.service.glean.testing.GleanTestRule
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.GleanMetrics.Collections
import org.mozilla.fenix.GleanMetrics.Events
import org.mozilla.fenix.GleanMetrics.Tab
import org.mozilla.fenix.GleanMetrics.TabsTray
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.ext.maxActiveTime
import org.mozilla.fenix.ext.potentialInactiveTabs
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.home.HomeFragment
import org.mozilla.fenix.selection.SelectionHolder
import org.mozilla.fenix.tabstray.TrayPagerAdapter.Companion.INACTIVE_TABS_FEATURE_NAME
import org.mozilla.fenix.utils.Settings
import mozilla.components.browser.storage.sync.Tab as SyncTab

@RunWith(FenixRobolectricTestRunner::class) // for gleanTestRule
class DefaultTabsTrayControllerTest {

    private val activity: HomeActivity = mockk(relaxed = true)
    private val appStore: AppStore = mockk(relaxed = true)
    private val trayStore: TabsTrayStore = mockk(relaxed = true)
    private val browserStore: BrowserStore = mockk(relaxed = true)
    private val browsingModeManager: BrowsingModeManager = mockk(relaxed = true)
    private val navController: NavController = mockk(relaxed = true)
    private val tabsUseCases: TabsUseCases = mockk(relaxed = true)
    private val settings: Settings = mockk(relaxed = true)
    private val selectTab: TabsUseCases.SelectTabUseCase = mockk(relaxed = true)

    @MockK(relaxed = true)
    private lateinit var profiler: Profiler

    @get:Rule
    val gleanTestRule = GleanTestRule(testContext)

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `GIVEN private mode WHEN handleOpeningNewTab is called THEN a profile marker is added for the operations executed`() {
        var trayDismissed = false
        profiler = spyk(profiler) {
            every { getProfilerTime() } returns Double.MAX_VALUE
        }

        assertNull(TabsTray.newPrivateTabTapped.testGetValue())
        assertNull(TabsTray.closed.testGetValue())

        createController(
            dismissTray = {
                trayDismissed = true
            },
        ).handleOpeningNewTab(true)

        assertNotNull(TabsTray.newPrivateTabTapped.testGetValue())

        verifyOrder {
            profiler.getProfilerTime()
            navController.navigate(
                TabsTrayFragmentDirections.actionGlobalHome(focusOnAddressBar = true),
            )
            profiler.addMarker(
                "DefaultTabTrayController.onNewTabTapped",
                Double.MAX_VALUE,
            )
        }

        assertTrue(trayDismissed)
        assertNotNull(TabsTray.closed.testGetValue())
    }

    @Test
    fun `GIVEN normal mode WHEN handleOpeningNewTab is called THEN a profile marker is added for the operations executed`() {
        var trayDismissed = false
        profiler = spyk(profiler) {
            every { getProfilerTime() } returns Double.MAX_VALUE
        }

        createController(
            dismissTray = {
                trayDismissed = true
            },
        ).handleOpeningNewTab(false)

        verifyOrder {
            profiler.getProfilerTime()
            navController.navigate(
                TabsTrayFragmentDirections.actionGlobalHome(focusOnAddressBar = true),
            )
            profiler.addMarker(
                "DefaultTabTrayController.onNewTabTapped",
                Double.MAX_VALUE,
            )
        }

        assertTrue(trayDismissed)
        assertNotNull(TabsTray.closed.testGetValue())
    }

    @Test
    fun `GIVEN private mode WHEN handleOpeningNewTab is called THEN Event#NewPrivateTabTapped is added to telemetry`() {
        assertNull(TabsTray.newPrivateTabTapped.testGetValue())

        createController().handleOpeningNewTab(true)

        assertNotNull(TabsTray.newPrivateTabTapped.testGetValue())
    }

    @Test
    fun `GIVEN private mode WHEN handleOpeningNewTab is called THEN Event#NewTabTapped is added to telemetry`() {
        assertNull(TabsTray.newTabTapped.testGetValue())

        createController().handleOpeningNewTab(false)

        assertNotNull(TabsTray.newTabTapped.testGetValue())
    }

    @Test
    fun `WHEN a tab is clicked to be opened THEN report the metric and notify the listener and open the tab`() {
        val controller = spyk(createController())
        assertNull(TabsTray.openedExistingTab.testGetValue())
        controller.handleTabOpen("123", null)

        assertNotNull(TabsTray.openedExistingTab.testGetValue())
        verify { selectTab.invoke("123") }
        verify { controller.handleNavigateToBrowser() }
    }

    @Test
    fun `GIVEN there are no currently-selected tabs WHEN a tab is long clicked THEN select the tab and report the metric and return true`() {
        val controller = spyk(createController())
        val selectionHolder = mockk<SelectionHolder<TabSessionState>> {
            every { selectedItems } returns emptySet()
        }
        val tab: TabSessionState = mockk()
        assertNull(Collections.longPress.testGetValue())
        assertTrue(controller.handleTabLongClick(tab, selectionHolder))

        assertNotNull(Collections.longPress.testGetValue())
        verify { controller.handleTabSelected(tab) }
    }

    @Test
    fun `GIVEN there are currently-selected tabs WHEN a tab is long clicked THEN return false`() {
        val controller = spyk(createController())
        val tab: TabSessionState = mockk()
        val selectionHolder = mockk<SelectionHolder<TabSessionState>> {
            every { selectedItems } returns setOf(tab)
        }
        assertFalse(controller.handleTabLongClick(tab, selectionHolder))
    }

    @Test
    fun `WHEN a tab is selected THEN emit the action`() {
        val tab: TabSessionState = mockk()
        createController().handleTabSelected(tab)

        verify { trayStore.dispatch(TabsTrayAction.AddSelectTab(tab)) }
    }

    @Test
    fun `WHEN a tab is unselected THEN emit the action`() {
        val tab: TabSessionState = mockk()
        createController().handleTabUnselected(tab)

        verify { trayStore.dispatch(TabsTrayAction.RemoveSelectTab(tab)) }
    }

    @Test
    fun `GIVEN multi-selection is not enabled WHEN a tab is selected THEN open the tab`() {
        val controller = spyk(createController())
        val selectionHolder = mockk<SelectionHolder<TabSessionState>> {
            every { selectedItems } returns emptySet()
        }
        val tab: TabSessionState = mockk {
            every { id } returns "123"
        }
        every { trayStore.state.mode } returns TabsTrayState.Mode.Normal

        controller.handleMultiSelectTabClick(
            tab = tab,
            holder = selectionHolder,
            source = "source",
        )

        verify { controller.handleTabOpen("123", "source") }
    }

    @Test
    fun `GIVEN multi-selection is enabled WHEN a tab is selected THEN select the tab`() {
        val controller = spyk(createController())
        val tab: TabSessionState = mockk {
            every { id } returns "123"
        }
        val selectionHolder = mockk<SelectionHolder<TabSessionState>> {
            every { selectedItems } returns emptySet()
        }
        every { trayStore.state.mode } returns TabsTrayState.Mode.Select(setOf())

        controller.handleMultiSelectTabClick(
            tab = tab,
            holder = selectionHolder,
            source = "source",
        )

        verify { controller.handleTabSelected(tab) }
    }

    @Test
    fun `GIVEN multi-selection is enabled WHEN a selected tab is unselected THEN unselect the tab`() {
        val controller = spyk(createController())
        val tab: TabSessionState = mockk {
            every { id } returns "123"
        }
        val selectionHolder = mockk<SelectionHolder<TabSessionState>> {
            every { selectedItems } returns setOf(tab)
        }
        every { trayStore.state.mode } returns TabsTrayState.Mode.Select(setOf(tab))

        controller.handleMultiSelectTabClick(
            tab = tab,
            holder = selectionHolder,
            source = "source",
        )

        verify { controller.handleTabUnselected(tab) }
    }

    @Test
    fun `WHEN handleTabDeletion is called THEN Event#ClosedExistingTab is added to telemetry`() {
        val tab: TabSessionState = mockk { every { content.private } returns true }
        assertNull(TabsTray.closedExistingTab.testGetValue())

        every { browserStore.state } returns mockk()
        try {
            mockkStatic("mozilla.components.browser.state.selector.SelectorsKt")
            every { browserStore.state.findTab(any()) } returns tab
            every { browserStore.state.getNormalOrPrivateTabs(any()) } returns listOf(tab)

            createController().handleTabDeletion("testTabId", "unknown")
            assertNotNull(TabsTray.closedExistingTab.testGetValue())
        } finally {
            unmockkStatic("mozilla.components.browser.state.selector.SelectorsKt")
        }
    }

    @Test
    fun `GIVEN active private download WHEN handleTabDeletion is called for the last private tab THEN showCancelledDownloadWarning is called`() {
        var showCancelledDownloadWarningInvoked = false
        val controller = spyk(
            createController(
                showCancelledDownloadWarning = { _, _, _ ->
                    showCancelledDownloadWarningInvoked = true
                },
            ),
        )
        val tab: TabSessionState = mockk { every { content.private } returns true }
        every { browserStore.state } returns mockk()
        every { browserStore.state.downloads } returns mapOf(
            "1" to DownloadState(
                "https://mozilla.org/download",
                private = true,
                destinationDirectory = "Download",
                status = DownloadState.Status.DOWNLOADING,
            ),
        )
        try {
            mockkStatic("mozilla.components.browser.state.selector.SelectorsKt")
            every { browserStore.state.findTab(any()) } returns tab
            every { browserStore.state.getNormalOrPrivateTabs(any()) } returns listOf(tab)

            controller.handleTabDeletion("testTabId", "unknown")

            assertTrue(showCancelledDownloadWarningInvoked)
        } finally {
            unmockkStatic("mozilla.components.browser.state.selector.SelectorsKt")
        }
    }

    @Test
    fun `WHEN handleTrayScrollingToPosition is called with smoothScroll=true THEN it scrolls to that position with smoothScroll`() {
        var selectTabPositionInvoked = false
        createController(
            selectTabPosition = { position, smoothScroll ->
                assertEquals(3, position)
                assertTrue(smoothScroll)
                selectTabPositionInvoked = true
            },
        ).handleTrayScrollingToPosition(3, true)

        assertTrue(selectTabPositionInvoked)
    }

    @Test
    fun `WHEN handleTrayScrollingToPosition is called with smoothScroll=true THEN it emits an action for the tray page of that tab position`() {
        createController().handleTrayScrollingToPosition(33, true)

        verify { trayStore.dispatch(TabsTrayAction.PageSelected(Page.positionToPage(33))) }
    }

    @Test
    fun `WHEN handleTrayScrollingToPosition is called with smoothScroll=false THEN it emits an action for the tray page of that tab position`() {
        createController().handleTrayScrollingToPosition(44, true)

        verify { trayStore.dispatch(TabsTrayAction.PageSelected(Page.positionToPage(44))) }
    }

    @Test
    fun `GIVEN already on browserFragment WHEN handleNavigateToBrowser is called THEN the tray is dismissed`() {
        every { navController.currentDestination?.id } returns R.id.browserFragment

        var dismissTrayInvoked = false
        createController(dismissTray = { dismissTrayInvoked = true }).handleNavigateToBrowser()

        assertTrue(dismissTrayInvoked)
        verify(exactly = 0) { navController.popBackStack() }
        verify(exactly = 0) { navController.popBackStack(any(), any()) }
        verify(exactly = 0) { navController.navigate(any<Int>()) }
        verify(exactly = 0) { navController.navigate(any<NavDirections>()) }
        verify(exactly = 0) { navController.navigate(any<NavDirections>(), any<NavOptions>()) }
    }

    @Test
    fun `GIVEN not already on browserFragment WHEN handleNavigateToBrowser is called THEN the tray is dismissed and popBackStack is executed`() {
        every { navController.currentDestination?.id } returns R.id.browserFragment + 1
        every { navController.popBackStack(R.id.browserFragment, false) } returns true

        var dismissTrayInvoked = false
        createController(dismissTray = { dismissTrayInvoked = true }).handleNavigateToBrowser()

        assertTrue(dismissTrayInvoked)
        verify { navController.popBackStack(R.id.browserFragment, false) }
        verify(exactly = 0) { navController.navigate(any<Int>()) }
        verify(exactly = 0) { navController.navigate(any<NavDirections>()) }
        verify(exactly = 0) { navController.navigate(any<NavDirections>(), any<NavOptions>()) }
    }

    @Test
    fun `GIVEN not already on browserFragment WHEN handleNavigateToBrowser is called and popBackStack fails THEN it navigates to browserFragment`() {
        every { navController.currentDestination?.id } returns R.id.browserFragment + 1
        every { navController.popBackStack(R.id.browserFragment, false) } returns false

        var dismissTrayInvoked = false
        createController(dismissTray = { dismissTrayInvoked = true }).handleNavigateToBrowser()

        assertTrue(dismissTrayInvoked)
        verify { navController.popBackStack(R.id.browserFragment, false) }
        verify { navController.navigate(R.id.browserFragment) }
    }

    @Test
    fun `GIVEN not already on browserFragment WHEN handleNavigateToBrowser is called and popBackStack succeeds THEN the method finishes`() {
        every { navController.popBackStack(R.id.browserFragment, false) } returns true

        var dismissTrayInvoked = false
        createController(dismissTray = { dismissTrayInvoked = true }).handleNavigateToBrowser()

        assertTrue(dismissTrayInvoked)
        verify(exactly = 1) { navController.popBackStack(R.id.browserFragment, false) }
        verify(exactly = 0) { navController.navigate(R.id.browserFragment) }
    }

    @Test
    fun `GIVEN more tabs opened WHEN handleTabDeletion is called THEN that tab is removed and an undo snackbar is shown`() {
        val tab: TabSessionState = mockk {
            every { content } returns mockk()
            every { content.private } returns true
        }
        every { browserStore.state } returns mockk()
        try {
            mockkStatic("mozilla.components.browser.state.selector.SelectorsKt")
            every { browserStore.state.findTab(any()) } returns tab
            every { browserStore.state.getNormalOrPrivateTabs(any()) } returns listOf(tab, mockk())

            var showUndoSnackbarForTabInvoked = false
            createController(
                showUndoSnackbarForTab = {
                    assertTrue(it)
                    showUndoSnackbarForTabInvoked = true
                },
            ).handleTabDeletion("22")

            verify { tabsUseCases.removeTab("22") }
            assertTrue(showUndoSnackbarForTabInvoked)
        } finally {
            unmockkStatic("mozilla.components.browser.state.selector.SelectorsKt")
        }
    }

    @Test
    fun `GIVEN only one tab opened WHEN handleTabDeletion is called THEN that it navigates to home where the tab will be removed`() {
        var showUndoSnackbarForTabInvoked = false
        val controller = spyk(createController(showUndoSnackbarForTab = { showUndoSnackbarForTabInvoked = true }))
        val tab: TabSessionState = mockk {
            every { content } returns mockk()
            every { content.private } returns true
        }
        every { browserStore.state } returns mockk()
        try {
            mockkStatic("mozilla.components.browser.state.selector.SelectorsKt")
            every { browserStore.state.findTab(any()) } returns tab
            every { browserStore.state.getNormalOrPrivateTabs(any()) } returns listOf(tab)

            controller.handleTabDeletion("33")

            verify { controller.dismissTabsTrayAndNavigateHome("33") }
            verify(exactly = 0) { tabsUseCases.removeTab(any()) }
            assertFalse(showUndoSnackbarForTabInvoked)
        } finally {
            unmockkStatic("mozilla.components.browser.state.selector.SelectorsKt")
        }
    }

    @Test
    fun `WHEN handleMultipleTabsDeletion is called to close all private tabs THEN that it navigates to home where that tabs will be removed and shows undo snackbar`() {
        var showUndoSnackbarForTabInvoked = false
        val controller = spyk(
            createController(
                showUndoSnackbarForTab = {
                    assertTrue(it)
                    showUndoSnackbarForTabInvoked = true
                },
            ),
        )

        val privateTab = createTab(url = "url", private = true)

        every { browserStore.state } returns mockk()
        try {
            mockkStatic("mozilla.components.browser.state.selector.SelectorsKt")
            every { browserStore.state.getNormalOrPrivateTabs(any()) } returns listOf(mockk(), mockk())

            controller.handleMultipleTabsDeletion(listOf(privateTab, mockk()))

            assertNotNull(TabsTray.closeSelectedTabs.testGetValue())
            val snapshot = TabsTray.closeSelectedTabs.testGetValue()!!
            assertEquals(1, snapshot.size)
            assertEquals("2", snapshot.single().extra?.getValue("tab_count"))

            verify { controller.dismissTabsTrayAndNavigateHome(HomeFragment.ALL_PRIVATE_TABS) }
            assertTrue(showUndoSnackbarForTabInvoked)
            verify(exactly = 0) { tabsUseCases.removeTabs(any()) }
        } finally {
            unmockkStatic("mozilla.components.browser.state.selector.SelectorsKt")
        }
    }

    @Test
    fun `WHEN handleMultipleTabsDeletion is called to close all normal tabs THEN that it navigates to home where that tabs will be removed and shows undo snackbar`() {
        var showUndoSnackbarForTabInvoked = false
        val controller = spyk(
            createController(
                showUndoSnackbarForTab = {
                    assertFalse(it)
                    showUndoSnackbarForTabInvoked = true
                },
            ),
        )

        val normalTab = createTab(url = "url", private = false)

        every { browserStore.state } returns mockk()
        try {
            mockkStatic("mozilla.components.browser.state.selector.SelectorsKt")
            every { browserStore.state.getNormalOrPrivateTabs(any()) } returns listOf(mockk(), mockk())

            controller.handleMultipleTabsDeletion(listOf(normalTab, normalTab))

            assertNotNull(TabsTray.closeSelectedTabs.testGetValue())
            val snapshot = TabsTray.closeSelectedTabs.testGetValue()!!
            assertEquals(1, snapshot.size)
            assertEquals("2", snapshot.single().extra?.getValue("tab_count"))

            verify { controller.dismissTabsTrayAndNavigateHome(HomeFragment.ALL_NORMAL_TABS) }
            verify(exactly = 0) { tabsUseCases.removeTabs(any()) }
            assertTrue(showUndoSnackbarForTabInvoked)
        } finally {
            unmockkStatic("mozilla.components.browser.state.selector.SelectorsKt")
        }
    }

    @Test
    fun `WHEN handleMultipleTabsDeletion is called to close some private tabs THEN that it uses tabsUseCases#removeTabs and shows an undo snackbar`() {
        var showUndoSnackbarForTabInvoked = false
        val controller = spyk(createController(showUndoSnackbarForTab = { showUndoSnackbarForTabInvoked = true }))
        val privateTab = createTab(id = "42", url = "url", private = true)

        every { browserStore.state } returns mockk()
        try {
            mockkStatic("mozilla.components.browser.state.selector.SelectorsKt")
            every { browserStore.state.getNormalOrPrivateTabs(any()) } returns listOf(mockk(), mockk())

            controller.handleMultipleTabsDeletion(listOf(privateTab))

            assertNotNull(TabsTray.closeSelectedTabs.testGetValue())
            val snapshot = TabsTray.closeSelectedTabs.testGetValue()!!
            assertEquals(1, snapshot.size)
            assertEquals("1", snapshot.single().extra?.getValue("tab_count"))

            verify { tabsUseCases.removeTabs(listOf("42")) }
            verify(exactly = 0) { controller.dismissTabsTrayAndNavigateHome(any()) }
            assertTrue(showUndoSnackbarForTabInvoked)
        } finally {
            unmockkStatic("mozilla.components.browser.state.selector.SelectorsKt")
        }
    }

    @Test
    fun `WHEN handleMultipleTabsDeletion is called to close some normal tabs THEN that it uses tabsUseCases#removeTabs and shows an undo snackbar`() {
        var showUndoSnackbarForTabInvoked = false
        val controller = spyk(createController(showUndoSnackbarForTab = { showUndoSnackbarForTabInvoked = true }))
        val privateTab = createTab(id = "24", url = "url", private = false)

        every { browserStore.state } returns mockk()
        try {
            mockkStatic("mozilla.components.browser.state.selector.SelectorsKt")
            every { browserStore.state.getNormalOrPrivateTabs(any()) } returns listOf(mockk(), mockk())

            controller.handleMultipleTabsDeletion(listOf(privateTab))

            assertNotNull(TabsTray.closeSelectedTabs.testGetValue())
            val snapshot = TabsTray.closeSelectedTabs.testGetValue()!!
            assertEquals(1, snapshot.size)
            assertEquals("1", snapshot.single().extra?.getValue("tab_count"))

            verify { tabsUseCases.removeTabs(listOf("24")) }
            verify(exactly = 0) { controller.dismissTabsTrayAndNavigateHome(any()) }
            assertTrue(showUndoSnackbarForTabInvoked)
        } finally {
            unmockkStatic("mozilla.components.browser.state.selector.SelectorsKt")
        }
    }

    @Test
    fun `GIVEN private mode selected WHEN sendNewTabEvent is called THEN NewPrivateTabTapped is tracked in telemetry`() {
        createController().sendNewTabEvent(true)

        assertNotNull(TabsTray.newPrivateTabTapped.testGetValue())
    }

    @Test
    fun `GIVEN normal mode selected WHEN sendNewTabEvent is called THEN NewTabTapped is tracked in telemetry`() {
        assertNull(TabsTray.newTabTapped.testGetValue())

        createController().sendNewTabEvent(false)

        assertNotNull(TabsTray.newTabTapped.testGetValue())
    }

    @Test
    fun `WHEN dismissTabsTrayAndNavigateHome is called with a specific tab id THEN tray is dismissed and navigates home is opened to delete that tab`() {
        var dismissTrayInvoked = false
        var navigateToHomeAndDeleteSessionInvoked = false
        createController(
            dismissTray = {
                dismissTrayInvoked = true
            },
            navigateToHomeAndDeleteSession = {
                assertEquals("randomId", it)
                navigateToHomeAndDeleteSessionInvoked = true
            },
        ).dismissTabsTrayAndNavigateHome("randomId")

        assertTrue(dismissTrayInvoked)
        assertTrue(navigateToHomeAndDeleteSessionInvoked)
    }

    @Test
    fun `GIVEN a non-null targetId WHEN a tab is moved THEN move the tab`() {
        createController().handleTabsMove(
            tabId = "12",
            targetId = "13",
            placeAfter = true,
        )

        verify { tabsUseCases.moveTabs(listOf("12"), "13", true) }
    }

    @Test
    fun `GIVEN a null targetId WHEN a tab is moved THEN don't move the tab`() {
        createController().handleTabsMove(
            tabId = "12",
            targetId = null,
            placeAfter = true,
        )

        verify(exactly = 0) { tabsUseCases.moveTabs }
    }

    @Test
    fun `WHEN the recently closed item is clicked THEN close the tray and navigate to the recently closed flow`() {
        var dismissed = false
        val controller = createController(
            dismissTray = {
                dismissed = true
            },
        )

        controller.handleNavigateToRecentlyClosed()
        assertTrue(dismissed)
        verify { navController.navigate(R.id.recentlyClosedFragment) }
    }

    @Test
    fun `GIVEN media is playing WHEN the media button is clicked THEN report the metric and pause the media`() {
        val mediaSessionController = mockk<MediaSession.Controller>(relaxed = true)
        val playingTab: TabSessionState = mockk {
            every { mediaSessionState } returns mockk {
                every { playbackState } returns MediaSession.PlaybackState.PLAYING
                every { controller } returns mediaSessionController
            }
        }
        assertNull(Tab.mediaPause.testGetValue())

        createController().handleMediaClicked(playingTab)

        verify { mediaSessionController.pause() }
        assertNotNull(Tab.mediaPause.testGetValue())
    }

    @Test
    fun `GIVEN media is paused WHEN the media button is clicked THEN report the metric and play the media`() {
        val mediaSessionController = mockk<MediaSession.Controller>(relaxed = true)
        val pausedTab: TabSessionState = mockk {
            every { mediaSessionState } returns mockk {
                every { playbackState } returns MediaSession.PlaybackState.PAUSED
                every { controller } returns mediaSessionController
            }
        }
        assertNull(Tab.mediaPlay.testGetValue())

        createController().handleMediaClicked(pausedTab)

        verify { mediaSessionController.play() }
        assertNotNull(Tab.mediaPlay.testGetValue())
    }

    @Test
    fun `WHEN the inactive tabs section is expanded THEN the expanded telemetry event should be reported`() {
        assertNull(TabsTray.inactiveTabsExpanded.testGetValue())
        assertNull(TabsTray.inactiveTabsCollapsed.testGetValue())

        createController().updateCardExpansion(isExpanded = true)

        assertNotNull(TabsTray.inactiveTabsExpanded.testGetValue())
        assertNull(TabsTray.inactiveTabsCollapsed.testGetValue())
    }

    @Test
    fun `WHEN the inactive tabs section is collapsed THEN the collapsed telemetry event should be reported`() {
        assertNull(TabsTray.inactiveTabsExpanded.testGetValue())
        assertNull(TabsTray.inactiveTabsCollapsed.testGetValue())

        createController().updateCardExpansion(isExpanded = false)

        assertNull(TabsTray.inactiveTabsExpanded.testGetValue())
        assertNotNull(TabsTray.inactiveTabsCollapsed.testGetValue())
    }

    @Test
    fun `WHEN the inactive tabs auto-close feature prompt is dismissed THEN update settings and report the telemetry event`() {
        assertNull(TabsTray.autoCloseDimissed.testGetValue())

        createController().dismissAutoCloseDialog()

        assertNotNull(TabsTray.autoCloseDimissed.testGetValue())
        verify { settings.hasInactiveTabsAutoCloseDialogBeenDismissed = true }
    }

    @Test
    fun `WHEN the inactive tabs auto-close feature prompt is accepted THEN update settings and report the telemetry event`() {
        assertNull(TabsTray.autoCloseTurnOnClicked.testGetValue())

        createController().enableInactiveTabsAutoClose()

        assertNotNull(TabsTray.autoCloseTurnOnClicked.testGetValue())

        verify { settings.closeTabsAfterOneMonth = true }
        verify { settings.closeTabsAfterOneWeek = false }
        verify { settings.closeTabsAfterOneDay = false }
        verify { settings.manuallyCloseTabs = false }
        verify { settings.hasInactiveTabsAutoCloseDialogBeenDismissed = true }
    }

    @Test
    fun `WHEN an inactive tab is selected THEN report the telemetry event`() {
        val controller = createController()
        val tab = TabSessionState(
            id = "tabId",
            content = ContentState(
                url = "www.mozilla.com",
            ),
        )

        assertNull(TabsTray.openInactiveTab.testGetValue())

        controller.openInactiveTab(tab)

        assertNotNull(TabsTray.openInactiveTab.testGetValue())
    }

    @Test
    fun `WHEN an inactive tab is clicked to be closed THEN report the telemetry event and close the tab`() {
        val controller = spyk(createController()) {
            every { handleTabDeletion(any(), any()) } returns Unit
        }
        val tab: TabSessionState = mockk {
            every { id } returns "123"
        }

        assertNull(TabsTray.closeInactiveTab.testGetValue())

        controller.closeInactiveTab(tab)

        verify { controller.handleTabDeletion("123", INACTIVE_TABS_FEATURE_NAME) }
        assertNotNull(TabsTray.closeInactiveTab.testGetValue())
    }

    @Test
    fun `WHEN all inactive tabs are closed THEN perform the deletion and report the telemetry event and show a Snackbar`() {
        var showSnackbarInvoked = false
        val controller = createController(
            showUndoSnackbarForTab = {
                showSnackbarInvoked = true
            },
        )
        val inactiveTab: TabSessionState = mockk {
            every { lastAccess } returns maxActiveTime
            every { createdAt } returns 0
            every { id } returns "24"
            every { content } returns mockk {
                every { private } returns false
            }
        }

        try {
            mockkStatic("mozilla.components.browser.state.selector.SelectorsKt")
            every { browserStore.state } returns mockk()
            every { browserStore.state.potentialInactiveTabs } returns listOf(inactiveTab)
            assertNull(TabsTray.closeAllInactiveTabs.testGetValue())

            controller.deleteAllInactiveTabs()

            verify { tabsUseCases.removeTabs(listOf("24")) }
            assertNotNull(TabsTray.closeAllInactiveTabs.testGetValue())
            assertTrue(showSnackbarInvoked)
        } finally {
            unmockkStatic("mozilla.components.browser.state.selector.SelectorsKt")
        }
    }

    @Test
    fun `WHEN a synced tab is clicked THEN report the metric and open the browser`() {
        val tab = mockk<SyncTab>()
        val entry = mockk<TabEntry>()
        assertNull(Events.syncedTabOpened.testGetValue())

        every { tab.active() }.answers { entry }
        every { entry.url }.answers { "https://mozilla.org" }

        var dismissTabTrayInvoked = false
        createController(
            dismissTray = {
                dismissTabTrayInvoked = true
            },
        ).handleSyncedTabClick(tab)

        assertTrue(dismissTabTrayInvoked)
        assertNotNull(Events.syncedTabOpened.testGetValue())

        verify {
            activity.openToBrowserAndLoad(
                searchTermOrURL = "https://mozilla.org",
                newTab = true,
                from = BrowserDirection.FromTabsTray,
            )
        }
    }

    @Test
    fun `GIVEN tab multi-selection is active WHEN the back button is pressed THEN emit the action and return true`() {
        every { trayStore.state.mode } returns TabsTrayState.Mode.Select(setOf())

        assertTrue(createController().handleOnBackPressed())
        verify { trayStore.dispatch(TabsTrayAction.ExitSelectMode) }
    }

    @Test
    fun `GIVEN tab multi-selection is not active WHEN the back button is pressed THEN return false`() {
        every { trayStore.state.mode } returns TabsTrayState.Mode.Normal
        assertFalse(createController().handleOnBackPressed())
    }

    private fun createController(
        navigateToHomeAndDeleteSession: (String) -> Unit = { },
        selectTabPosition: (Int, Boolean) -> Unit = { _, _ -> },
        dismissTray: () -> Unit = { },
        showUndoSnackbarForTab: (Boolean) -> Unit = { _ -> },
        showCancelledDownloadWarning: (Int, String?, String?) -> Unit = { _, _, _ -> },
    ): DefaultTabsTrayController {
        return DefaultTabsTrayController(
            activity = activity,
            appStore = appStore,
            trayStore = trayStore,
            browserStore = browserStore,
            browsingModeManager = browsingModeManager,
            navController = navController,
            navigateToHomeAndDeleteSession = navigateToHomeAndDeleteSession,
            profiler = profiler,
            tabsUseCases = tabsUseCases,
            selectTabPosition = selectTabPosition,
            dismissTray = dismissTray,
            showUndoSnackbarForTab = showUndoSnackbarForTab,
            showCancelledDownloadWarning = showCancelledDownloadWarning,
            settings = settings,
            selectTab = selectTab,
        )
    }
}
