/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.EngineView
import mozilla.components.feature.search.SearchUseCases
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.feature.top.sites.TopSitesUseCases
import mozilla.components.support.test.rule.MainCoroutineRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.BrowserAnimator
import org.mozilla.fenix.browser.BrowserFragmentDirections
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager
import org.mozilla.fenix.browser.browsingmode.DefaultBrowsingModeManager
import org.mozilla.fenix.browser.readermode.ReaderModeController
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.directionsEq
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(FenixRobolectricTestRunner::class)
class DefaultBrowserToolbarControllerTest {

    @get:Rule
    val coroutinesTestRule = MainCoroutineRule()

    @RelaxedMockK private lateinit var activity: HomeActivity
    @RelaxedMockK private lateinit var navController: NavController
    @RelaxedMockK private lateinit var onTabCounterClicked: () -> Unit
    @RelaxedMockK private lateinit var onCloseTab: (Session) -> Unit
    @RelaxedMockK private lateinit var sessionManager: SessionManager
    @RelaxedMockK private lateinit var engineView: EngineView
    @RelaxedMockK private lateinit var currentSession: Session
    @RelaxedMockK private lateinit var metrics: MetricController
    @RelaxedMockK private lateinit var searchUseCases: SearchUseCases
    @RelaxedMockK private lateinit var sessionUseCases: SessionUseCases
    @RelaxedMockK private lateinit var browserAnimator: BrowserAnimator
    @RelaxedMockK private lateinit var topSitesUseCase: TopSitesUseCases
    @RelaxedMockK private lateinit var readerModeController: ReaderModeController

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

        val onComplete = slot<() -> Unit>()
        every { browserAnimator.captureEngineViewAndDrawStatically(capture(onComplete)) } answers { onComplete.captured.invoke() }
    }

    @Test
    fun handleBrowserToolbarPaste() = runBlockingTest {
        every { currentSession.id } returns "1"

        val pastedText = "Mozilla"
        val controller = createController()
        controller.handleToolbarPaste(pastedText)

        val directions = BrowserFragmentDirections.actionBrowserFragmentToSearchFragment(
            sessionId = "1",
            pastedText = pastedText
        )
        verify {
            navController.navigate(directionsEq(directions), any<NavOptions>())
        }
    }

    @Test
    fun handleBrowserToolbarPasteAndGoSearch() = runBlockingTest {
        val pastedText = "Mozilla"

        val controller = createController()
        controller.handleToolbarPasteAndGo(pastedText)
        verifyOrder {
            currentSession.searchTerms = "Mozilla"
            searchUseCases.defaultSearch.invoke(pastedText, currentSession)
        }
    }

    @Test
    fun handleBrowserToolbarPasteAndGoUrl() = runBlockingTest {
        val pastedText = "https://mozilla.org"

        val controller = createController()
        controller.handleToolbarPasteAndGo(pastedText)
        verifyOrder {
            currentSession.searchTerms = ""
            sessionUseCases.loadUrl(pastedText)
        }
    }

    @Test
    fun handleTabCounterClick() = runBlockingTest {
        val controller = createController()
        controller.handleTabCounterClick()

        verify { onTabCounterClicked() }
    }

    @Test
    fun `handle reader mode enabled`() = runBlockingTest {
        val controller = createController()
        controller.handleReaderModePressed(enabled = true)

        verify { readerModeController.showReaderView() }
    }

    @Test
    fun `handle reader mode disabled`() = runBlockingTest {
        val controller = createController()
        controller.handleReaderModePressed(enabled = false)

        verify { readerModeController.hideReaderView() }
    }

    @Test
    fun handleToolbarClick() = runBlockingTest {
        every { currentSession.id } returns "1"

        val controller = createController()
        controller.handleToolbarClick()

        val directions = BrowserFragmentDirections.actionBrowserFragmentToSearchFragment(
            sessionId = "1"
        )
        verify { metrics.track(Event.SearchBarTapped(Event.SearchBarTapped.Source.BROWSER)) }
        verify {
            navController.navigate(directionsEq(directions), any<NavOptions>())
        }
    }

    @Test
    fun handleToolbarCloseTabPressWithLastPrivateSession() = runBlockingTest {
        every { currentSession.id } returns "1"
        val browsingModeManager = object : BrowsingModeManager {
            override var mode = BrowsingMode.Private
        }
        val item = TabCounterMenuItem.CloseTab
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
        verify { navController.navigate(BrowserFragmentDirections.actionGlobalHome(sessionToDelete = "1")) }
        assertEquals(BrowsingMode.Normal, browsingModeManager.mode)
    }

    @Test
    fun handleToolbarCloseTabPress() = runBlockingTest {
        val tabsUseCases: TabsUseCases = mockk(relaxed = true)
        val removeTabUseCase: TabsUseCases.RemoveTabUseCase = mockk(relaxed = true)
        val item = TabCounterMenuItem.CloseTab

        every { sessionManager.sessions } returns emptyList()
        every { activity.components.useCases.tabsUseCases } returns tabsUseCases
        every { tabsUseCases.removeTab } returns removeTabUseCase

        val controller = createController()
        controller.handleTabCounterItemInteraction(item)
        verify { removeTabUseCase.invoke(currentSession) }
    }

    @Test
    fun handleToolbarNewTabPress() = runBlockingTest {
        val browsingModeManager: BrowsingModeManager = DefaultBrowsingModeManager(
            BrowsingMode.Private,
            mockk(relaxed = true)
        ) {}
        val item = TabCounterMenuItem.NewTab(false)

        every { activity.browsingModeManager } returns browsingModeManager

        val controller = createController()
        controller.handleTabCounterItemInteraction(item)
        assertEquals(BrowsingMode.Normal, activity.browsingModeManager.mode)
        verify { navController.popBackStack(R.id.homeFragment, false) }
    }

    @Test
    fun handleToolbarNewPrivateTabPress() = runBlockingTest {
        val browsingModeManager: BrowsingModeManager = DefaultBrowsingModeManager(
            BrowsingMode.Normal,
            mockk(relaxed = true)
        ) {}
        val item = TabCounterMenuItem.NewTab(true)

        every { activity.browsingModeManager } returns browsingModeManager

        val controller = createController()
        controller.handleTabCounterItemInteraction(item)
        assertEquals(BrowsingMode.Private, activity.browsingModeManager.mode)
        verify { navController.popBackStack(R.id.homeFragment, false) }
    }

    private fun createController(
        activity: HomeActivity = this.activity,
        customTabSession: Session? = null,
        useNewSearchExperience: Boolean = false
    ) = DefaultBrowserToolbarController(
        activity = activity,
        navController = navController,
        metrics = metrics,
        engineView = engineView,
        browserAnimator = browserAnimator,
        customTabSession = customTabSession,
        readerModeController = readerModeController,
        sessionManager = sessionManager,
        useNewSearchExperience = useNewSearchExperience,
        onTabCounterClicked = onTabCounterClicked,
        onCloseTab = onCloseTab
    )
}
