/*  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.search

import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import mozilla.components.browser.search.SearchEngine
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.TestApplication
import org.mozilla.fenix.browser.BrowserNavigation
import org.mozilla.fenix.browser.DirectionsProvider
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.browser.browsingmode.DefaultBrowsingModeManager
import org.mozilla.fenix.components.UseCases
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.metrics
import org.mozilla.fenix.ext.searchEngineManager
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.utils.Settings
import org.mozilla.fenix.whatsnew.clear
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = TestApplication::class)
class DefaultSearchControllerTest {

    private val context: HomeActivity = mockk(relaxed = true)
    private val store: SearchFragmentStore = mockk(relaxed = true)
    private val navController: NavController = mockk(relaxed = true)
    private val defaultSearchEngine: SearchEngine? = mockk(relaxed = true)
    private val session: Session? = mockk(relaxed = true)
    private val searchEngine: SearchEngine = mockk(relaxed = true)
    private val metrics: MetricController = mockk(relaxed = true)
    private val sessionManager: SessionManager = mockk(relaxed = true)
    private var useCases: UseCases = mockk(relaxed = true)
    private var navHost: NavHostFragment = mockk(relaxed = true)
    private var createSessionObserver: () -> Unit = mockk(relaxed = true)
    private var directionsProvider: DirectionsProvider = mockk(relaxed = true)

    private lateinit var controller: DefaultSearchController
    private lateinit var settings: Settings

    @Before
    fun setUp() {
        every { context.searchEngineManager.defaultSearchEngine } returns defaultSearchEngine
        every { store.state.session } returns session
        every { store.state.searchEngineSource.searchEngine } returns searchEngine
        every { context.metrics } returns metrics
        every { context.components.core.sessionManager } returns sessionManager

        controller = DefaultSearchController(
            context = context,
            store = store,
            navController = navController
        )

        settings = testContext.settings().apply { testContext.settings().clear() }
    }

    @Test
    fun handleUrlCommitted() {
        val url = "https://www.google.com/"
        mockkObject(BrowserNavigation)
        BrowserNavigation.init(navHost, useCases, directionsProvider, createSessionObserver)
        DefaultBrowsingModeManager.initMode(BrowsingMode.Normal)
        every { createSessionObserver.invoke() } just Runs
        every { BrowserNavigation.openToBrowserAndLoad(any(), any(), any(), any()) } just Runs

        controller.handleUrlCommitted(url)

        verify {
            BrowserNavigation.openToBrowserAndLoad(
                searchTermOrURL = url,
                newTab = session == null,
                from = BrowserDirection.FromSearch,
                engine = searchEngine
            )
        }
        verify { metrics.track(Event.EnteredUrl(false)) }

        unmockkObject(BrowserNavigation)
    }

    @Test
    fun handleEditingCancelled() {
        controller.handleEditingCancelled()

        verify { navController.navigateUp() }
    }

    @Test
    fun handleTextChangedNonEmpty() {
        val text = "fenix"
        DefaultBrowsingModeManager.initMode(BrowsingMode.Normal)

        controller.handleTextChanged(text)

        verify { store.dispatch(SearchFragmentAction.UpdateQuery(text)) }
    }

    @Test
    fun handleTextChangedEmpty() {
        val text = ""

        controller.handleTextChanged(text)

        verify { store.dispatch(SearchFragmentAction.UpdateQuery(text)) }
    }

    @Test
    fun `show search shortcuts when setting enabled AND query empty`() {
        val text = ""

        controller.handleTextChanged(text)

        verify { store.dispatch(SearchFragmentAction.ShowSearchShortcutEnginePicker(true)) }
    }

    @Test
    fun `do not show search shortcuts when setting enabled AND query non-empty`() {
        val text = "mozilla"
        DefaultBrowsingModeManager.initMode(BrowsingMode.Normal)

        controller.handleTextChanged(text)

        verify { store.dispatch(SearchFragmentAction.ShowSearchShortcutEnginePicker(false)) }
    }

    @Test
    fun `do not show search shortcuts when setting disabled AND query empty`() {
        testContext.settings().preferences
            .edit()
            .putBoolean(testContext.getString(R.string.pref_key_show_search_shortcuts), false)
            .apply()

        assertFalse(testContext.settings().shouldShowSearchShortcuts)

        val text = ""

        controller.handleTextChanged(text)

        verify { store.dispatch(SearchFragmentAction.ShowSearchShortcutEnginePicker(false)) }
    }

    @Test
    fun `do not show search shortcuts when setting disabled AND query non-empty`() {
        DefaultBrowsingModeManager.initMode(BrowsingMode.Normal)
        testContext.settings().preferences
            .edit()
            .putBoolean(testContext.getString(R.string.pref_key_show_search_shortcuts), false)
            .apply()

        assertFalse(testContext.settings().shouldShowSearchShortcuts)

        val text = "mozilla"

        controller.handleTextChanged(text)

        verify { store.dispatch(SearchFragmentAction.ShowSearchShortcutEnginePicker(false)) }
    }

    @Test
    fun handleUrlTapped() {
        val url = "https://www.google.com/"
        mockkObject(BrowserNavigation)
        BrowserNavigation.init(navHost, useCases, directionsProvider, createSessionObserver)
        DefaultBrowsingModeManager.initMode(BrowsingMode.Normal)
        every { createSessionObserver.invoke() } just Runs
        every { BrowserNavigation.openToBrowserAndLoad(any(), any(), any()) } just Runs

        controller.handleUrlTapped(url)

        verify {
            BrowserNavigation.openToBrowserAndLoad(
                searchTermOrURL = url,
                newTab = session == null,
                from = BrowserDirection.FromSearch
            )
        }
        verify { metrics.track(Event.EnteredUrl(false)) }

        unmockkObject(BrowserNavigation)
    }

    @Test
    fun handleSearchTermsTapped() {
        val searchTerms = "fenix"
        mockkObject(BrowserNavigation)
        BrowserNavigation.init(navHost, useCases, directionsProvider, createSessionObserver)
        DefaultBrowsingModeManager.initMode(BrowsingMode.Normal)
        every { createSessionObserver.invoke() } just Runs
        every { BrowserNavigation.openToBrowserAndLoad(any(), any(), any(), any(), any()) } just Runs

        controller.handleSearchTermsTapped(searchTerms)

        verify {
            BrowserNavigation.openToBrowserAndLoad(
                searchTermOrURL = searchTerms,
                newTab = session == null,
                from = BrowserDirection.FromSearch,
                engine = searchEngine,
                forceSearch = true
            )
        }

        unmockkObject(BrowserNavigation)
    }

    @Test
    fun handleSearchShortcutEngineSelected() {
        val searchEngine: SearchEngine = mockk(relaxed = true)

        controller.handleSearchShortcutEngineSelected(searchEngine)

        verify { store.dispatch(SearchFragmentAction.SearchShortcutEngineSelected(searchEngine)) }
        verify { metrics.track(Event.SearchShortcutSelected(searchEngine, false)) }
    }

    @Test
    fun handleClickSearchEngineSettings() {
        val directions: NavDirections =
            SearchFragmentDirections.actionSearchFragmentToSearchEngineFragment()

        controller.handleClickSearchEngineSettings()

        verify { navController.navigate(directions) }
    }

    @Test
    fun handleSearchShortcutsButtonClicked_alreadyOpen() {
        every { store.state.showSearchShortcuts } returns true

        controller.handleSearchShortcutsButtonClicked()

        verify { store.dispatch(SearchFragmentAction.ShowSearchShortcutEnginePicker(false)) }
    }

    @Test
    fun handleSearchShortcutsButtonClicked_notYetOpen() {
        every { store.state.showSearchShortcuts } returns false

        controller.handleSearchShortcutsButtonClicked()

        verify { store.dispatch(SearchFragmentAction.ShowSearchShortcutEnginePicker(true)) }
    }

    @Test
    fun handleExistingSessionSelected() {
        val session: Session = mockk(relaxed = true)

        controller.handleExistingSessionSelected(session)

        verify { sessionManager.select(session) }
        verify { BrowserNavigation.openToBrowser(from = BrowserDirection.FromSearch) }
    }
}
