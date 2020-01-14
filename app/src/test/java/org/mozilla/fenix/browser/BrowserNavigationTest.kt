package org.mozilla.fenix.browser

import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import mozilla.components.browser.search.SearchEngine
import mozilla.components.browser.session.Session
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.R
import org.mozilla.fenix.TestApplication
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.browser.browsingmode.DefaultBrowsingModeManager
import org.mozilla.fenix.components.UseCases
import org.mozilla.fenix.ext.alreadyOnDestination
import org.mozilla.fenix.ext.nav
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class BrowserNavigationTest {
    private var useCases: UseCases = mockk(relaxed = true)
    private var navHost: NavHostFragment = mockk(relaxed = true)
    private var createSessionObserver: () -> Unit = mockk(relaxed = true)
    private var directionsProvider: DirectionsProvider = mockk(relaxed = true)

    @Before
    fun setUp() {
        mockkObject(BrowserNavigation)
    }

    @After
    fun cleanUp() {
        unmockkObject(BrowserNavigation)
    }

    @Test
    fun `given a valid BrowserNavigation object, when openToBrowserAndLoad() is called, then openToBrowser() and load() are called`() {
        // given
        BrowserNavigation.init(navHost, useCases, directionsProvider, createSessionObserver)
        every { BrowserNavigation.openToBrowser(BrowserDirection.FromGlobal, null) } just Runs
        every { BrowserNavigation.load(SEARCH_TERM, true, null, false) } just Runs

        // when
        BrowserNavigation.openToBrowserAndLoad(SEARCH_TERM, true, BrowserDirection.FromGlobal)

        // then
        verifyOrder {
            BrowserNavigation.openToBrowser(BrowserDirection.FromGlobal, null)
            BrowserNavigation.load(SEARCH_TERM, true, null, false)
        }
    }

    @Test
    fun `given an invalid BrowserNavigation object, when openToBrowserAndLoad() is called, then nothing should be called`() {
        // given
        BrowserNavigation.clearData()
        every { BrowserNavigation.openToBrowser(BrowserDirection.FromGlobal, null) } just Runs
        every { BrowserNavigation.load(SEARCH_TERM, true, null, false) } just Runs

        // when
        BrowserNavigation.openToBrowserAndLoad(SEARCH_TERM, true, BrowserDirection.FromGlobal)

        // then
        verify(exactly = 0) {
            BrowserNavigation.openToBrowser(BrowserDirection.FromGlobal, null)
            BrowserNavigation.load(SEARCH_TERM, true, null, false)
        }
    }

    @Test
    fun `given navHost alreadyOnDestination, when openToBrowser() is called, then the method should return`() {
        // given
        BrowserNavigation.init(navHost, useCases, directionsProvider, createSessionObserver)
        every { createSessionObserver() } just Runs
        every { navHost.navController.alreadyOnDestination(R.id.browserFragment) } returns true

        // when
        BrowserNavigation.openToBrowser(BrowserDirection.FromGlobal)

        // then
        verify { createSessionObserver() }
        verify(exactly = 0) { directionsProvider.getNavDirections(BrowserDirection.FromGlobal, null) }
    }

    @Test
    fun `given non nullable arguments and navHost !alreadyOnDestination, when openToBrowser() is called, then should navigate to given direction`() {
        // given
        val directions: NavDirections = mockk(relaxed = true)

        BrowserNavigation.init(navHost, useCases, directionsProvider, createSessionObserver)
        every { createSessionObserver() } just Runs
        every { navHost.navController.alreadyOnDestination(R.id.browserFragment) } returns false
        every { directionsProvider.getNavDirections(BrowserDirection.FromGlobal, SESSION_ID) } returns directions

        // when
        BrowserNavigation.openToBrowser(BrowserDirection.FromGlobal, SESSION_ID)

        // then
        verifyOrder {
            createSessionObserver()
            directionsProvider.getNavDirections(BrowserDirection.FromGlobal, SESSION_ID)
            navHost.navController.nav(null, directions)
        }
    }

    @Test
    fun `given newTab = true and forceSearch = true, when load() is called, then newTabSearch should be called`() {
        // given
        DefaultBrowsingModeManager.initMode(BrowsingMode.Normal)
        BrowserNavigation.init(navHost, useCases, directionsProvider, createSessionObserver)
        val engine: SearchEngine? = mockk(relaxed = true)

        every { useCases.searchUseCases.newTabSearch(any(), Session.Source.USER_ENTERED, any(), false, any()) } just Runs

        // when
        BrowserNavigation.load(SEARCH_TERM, true, engine, true)

        // then
        verify { useCases.searchUseCases.newTabSearch(any(), Session.Source.USER_ENTERED, any(), false, any()) }
    }

    @Test
    fun `given forceSearch = false and an url, when load() is called, then addTab should be called`() {
        // given
        DefaultBrowsingModeManager.initMode(BrowsingMode.Normal)
        BrowserNavigation.init(navHost, useCases, directionsProvider, createSessionObserver)
        val engine: SearchEngine? = mockk(relaxed = true)
        val searchTerm = URL

        // when
        BrowserNavigation.load(searchTerm, true, engine, false)

        // then
        verify { useCases.tabsUseCases.addTab.invoke(searchTerm) }
    }

    companion object {
        const val SEARCH_TERM = "test"
        const val URL = "http://mozilla.org"
        const val SESSION_ID = "id"
    }
}
