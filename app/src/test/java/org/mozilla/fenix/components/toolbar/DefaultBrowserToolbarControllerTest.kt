/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.EngineView
import mozilla.components.feature.search.SearchUseCases
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.top.sites.TopSitesUseCases
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.BrowserAnimator
import org.mozilla.fenix.browser.BrowserFragmentDirections
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.browser.browsingmode.SimpleBrowsingModeManager
import org.mozilla.fenix.browser.readermode.ReaderModeController
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.home.HomeScreenViewModel

@RunWith(FenixRobolectricTestRunner::class)
class DefaultBrowserToolbarControllerTest {

    @RelaxedMockK
    private lateinit var activity: HomeActivity
    @MockK(relaxUnitFun = true)
    private lateinit var navController: NavController
    @RelaxedMockK
    private lateinit var onTabCounterClicked: () -> Unit
    @RelaxedMockK
    private lateinit var onCloseTab: (Session) -> Unit
    @RelaxedMockK
    private lateinit var sessionManager: SessionManager
    @MockK(relaxUnitFun = true)
    private lateinit var engineView: EngineView
    @MockK
    private lateinit var currentSession: Session
    @RelaxedMockK
    private lateinit var metrics: MetricController
    @RelaxedMockK
    private lateinit var searchUseCases: SearchUseCases
    @RelaxedMockK
    private lateinit var sessionUseCases: SessionUseCases
    @RelaxedMockK
    private lateinit var browserAnimator: BrowserAnimator
    @RelaxedMockK
    private lateinit var topSitesUseCase: TopSitesUseCases
    @RelaxedMockK
    private lateinit var readerModeController: ReaderModeController
    @RelaxedMockK
    private lateinit var homeViewModel: HomeScreenViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { activity.components.useCases.sessionUseCases } returns sessionUseCases
        every { activity.components.useCases.searchUseCases } returns searchUseCases
        every { activity.components.useCases.topSitesUseCase } returns topSitesUseCase
        every { sessionManager.selectedSession } returns currentSession
        every { navController.currentDestination } returns mockk {
            every { id } returns R.id.browserFragment
        }
        every { currentSession.id } returns "1"
        every { currentSession.private } returns false
        every { currentSession.searchTerms = any() } just Runs

        val onComplete = slot<() -> Unit>()
        every { browserAnimator.captureEngineViewAndDrawStatically(capture(onComplete)) } answers { onComplete.captured.invoke() }
    }

    @Test
    fun handleBrowserToolbarPaste() {
        val pastedText = "Mozilla"
        val controller = createController()
        controller.handleToolbarPaste(pastedText)

        val directions = BrowserFragmentDirections.actionGlobalSearchDialog(
            sessionId = "1",
            pastedText = pastedText
        )

        verify { navController.navigate(directions, any<NavOptions>()) }
    }

    @Test
    fun handleBrowserToolbarPaste_useNewSearchExperience() {
        val pastedText = "Mozilla"
        val controller = createController()
        controller.handleToolbarPaste(pastedText)

        val directions = BrowserFragmentDirections.actionGlobalSearchDialog(
            sessionId = "1",
            pastedText = pastedText
        )

        verify { navController.navigate(directions, any<NavOptions>()) }
    }

    @Test
    fun handleBrowserToolbarPasteAndGoSearch() {
        val pastedText = "Mozilla"

        val controller = createController()
        controller.handleToolbarPasteAndGo(pastedText)
        verifyOrder {
            currentSession.searchTerms = "Mozilla"
            searchUseCases.defaultSearch.invoke(pastedText, currentSession)
        }
    }

    @Test
    fun handleBrowserToolbarPasteAndGoUrl() {
        val pastedText = "https://mozilla.org"

        val controller = createController()
        controller.handleToolbarPasteAndGo(pastedText)
        verifyOrder {
            currentSession.searchTerms = ""
            sessionUseCases.loadUrl(pastedText)
        }
    }

    @Test
    fun handleTabCounterClick() {
        val controller = createController()
        controller.handleTabCounterClick()

        verify { onTabCounterClicked() }
    }

    @Test
    fun `handle reader mode enabled`() {
        val controller = createController()
        controller.handleReaderModePressed(enabled = true)

        verify { readerModeController.showReaderView() }
    }

    @Test
    fun `handle reader mode disabled`() {
        val controller = createController()
        controller.handleReaderModePressed(enabled = false)

        verify { readerModeController.hideReaderView() }
    }

    @Test
    fun handleToolbarClick() {
        val controller = createController()
        controller.handleToolbarClick()

        val expected = BrowserFragmentDirections.actionGlobalSearchDialog(
            sessionId = "1"
        )

        verify { metrics.track(Event.SearchBarTapped(Event.SearchBarTapped.Source.BROWSER)) }
        verify { navController.navigate(expected, any<NavOptions>()) }
    }

    @Test
    fun handleToolbarClick_useNewSearchExperience() {
        val controller = createController()
        controller.handleToolbarClick()

        val expected = BrowserFragmentDirections.actionGlobalSearchDialog(
            sessionId = "1"
        )

        verify { metrics.track(Event.SearchBarTapped(Event.SearchBarTapped.Source.BROWSER)) }
        verify { navController.navigate(expected, any<NavOptions>()) }
    }

    @Test
    fun handleToolbarCloseTabPressWithLastPrivateSession() {
        val browsingModeManager = SimpleBrowsingModeManager(BrowsingMode.Private)
        val item = TabCounterMenu.Item.CloseTab
        val sessions = listOf(
            mockk<Session> {
                every { private } returns true
            }
        )

        every { currentSession.private } returns true
        every { sessionManager.sessions } returns sessions
        every { activity.browsingModeManager } returns browsingModeManager

        val controller = createController()
        controller.handleTabCounterItemInteraction(item)
        verify {
            homeViewModel.sessionToDelete = "1"
            navController.navigate(BrowserFragmentDirections.actionGlobalHome())
        }
        assertEquals(BrowsingMode.Normal, browsingModeManager.mode)
    }

    @Test
    fun handleToolbarCloseTabPress() {
        val item = TabCounterMenu.Item.CloseTab

        every { sessionManager.sessions } returns emptyList()

        val controller = createController()
        controller.handleTabCounterItemInteraction(item)
        verify { sessionManager.remove(currentSession, selectParentIfExists = true) }
    }

    @Test
    fun handleToolbarNewTabPress() {
        val browsingModeManager = SimpleBrowsingModeManager(BrowsingMode.Private)
        val item = TabCounterMenu.Item.NewTab(BrowsingMode.Normal)

        every { activity.browsingModeManager } returns browsingModeManager
        every { navController.popBackStack(R.id.homeFragment, any()) } returns true

        val controller = createController()
        controller.handleTabCounterItemInteraction(item)
        assertEquals(BrowsingMode.Normal, browsingModeManager.mode)
        verify { navController.popBackStack(R.id.homeFragment, false) }
    }

    @Test
    fun handleToolbarNewPrivateTabPress() {
        val browsingModeManager = SimpleBrowsingModeManager(BrowsingMode.Normal)
        val item = TabCounterMenu.Item.NewTab(BrowsingMode.Private)

        every { activity.browsingModeManager } returns browsingModeManager
        every { navController.popBackStack(R.id.homeFragment, any()) } returns true

        val controller = createController()
        controller.handleTabCounterItemInteraction(item)
        assertEquals(BrowsingMode.Private, browsingModeManager.mode)
        verify { navController.popBackStack(R.id.homeFragment, false) }
    }

    @Test
    fun `handleScroll for dynamic toolbars`() {
        val controller = createController()
        every { activity.settings().isDynamicToolbarEnabled } returns true

        controller.handleScroll(10)
        verify { engineView.setVerticalClipping(10) }
    }

    @Test
    fun `handleScroll for static toolbars`() {
        val controller = createController()
        every { activity.settings().isDynamicToolbarEnabled } returns false

        controller.handleScroll(10)
        verify(exactly = 0) { engineView.setVerticalClipping(10) }
    }

    private fun createController(
        activity: HomeActivity = this.activity,
        customTabSession: Session? = null
    ) = DefaultBrowserToolbarController(
        activity = activity,
        navController = navController,
        metrics = metrics,
        engineView = engineView,
        homeViewModel = homeViewModel,
        customTabSession = customTabSession,
        readerModeController = readerModeController,
        sessionManager = sessionManager,
        onTabCounterClicked = onTabCounterClicked,
        onCloseTab = onCloseTab
    )
}
