package org.mozilla.fenix.search

import androidx.navigation.NavController
import androidx.navigation.NavDirections
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mozilla.components.browser.search.SearchEngine
import mozilla.components.browser.search.SearchEngineManager
import org.junit.Assert.assertEquals
import mozilla.components.support.test.mock
import org.junit.Test
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity

class SearchInteractorTest {
    @Test
    fun onUrlCommitted() {
        val context: HomeActivity = mockk()
        val store: SearchStore = mockk()
        val state: SearchState = mockk()
        val searchEngineManager: SearchEngineManager = mockk(relaxed = true)
        val searchEngine = SearchEngineSource.Default(mockk())

        every { context.metrics } returns mockk(relaxed = true)
        every { context.searchEngineManager } returns searchEngineManager
        every { context.openToBrowserAndLoad(any(), any(), any(), any(), any(), any()) } just Runs

        every { store.state } returns state
        every { state.session } returns null
        every { state.searchEngineSource } returns searchEngine

        val interactor = SearchInteractor(context, mock(), store)

        interactor.onUrlCommitted("test")

        verify {
            context.openToBrowserAndLoad(
                searchTermOrURL = "test",
                newTab = true,
                from = BrowserDirection.FromSearch,
                engine = searchEngine.searchEngine
            )
        }
    }

    @Test
    fun onEditingCanceled() {
        val navController: NavController = mockk(relaxed = true)
        val interactor = SearchInteractor(mockk(), navController, mockk())

        interactor.onEditingCanceled()

        verify {
            navController.navigateUp()
        }
    }

    @Test
    fun onTextChanged() {
        val store = SearchStore(
            SearchState(
                query = "",
                showShortcutEnginePicker = false,
                searchEngineSource = SearchEngineSource.Default(mock()),
                showSuggestions = true,
                showVisitedSitesBookmarks = true,
                session = mock()
            ),
            ::searchStateReducer
        )

        val interactor = SearchInteractor(mockk(), mockk(), store)

        runBlocking {
            interactor.onTextChanged("test")
            delay(50)
        }

        assertEquals("test", store.state.query)
    }

    @Test
    fun onUrlTapped() {
        val context: HomeActivity = mockk()
        val store: SearchStore = mockk()
        val state: SearchState = mockk()

        every { context.metrics } returns mockk(relaxed = true)
        every { context.openToBrowserAndLoad(any(), any(), any()) } just Runs

        every { store.state } returns state
        every { state.session } returns null

        val interactor = SearchInteractor(context, mock(), store)

        interactor.onUrlTapped("test")

        verify {
            context.openToBrowserAndLoad(
                "test",
                true,
                BrowserDirection.FromSearch
            )
        }
    }

    @Test
    fun onSearchTermsTapped() {
        val context: HomeActivity = mockk()
        val store: SearchStore = mockk()
        val state: SearchState = mockk()
        val searchEngineManager: SearchEngineManager = mockk(relaxed = true)
        val searchEngine = SearchEngineSource.Default(mockk())

        every { context.metrics } returns mockk(relaxed = true)
        every { context.searchEngineManager } returns searchEngineManager
        every { context.openToBrowserAndLoad(any(), any(), any(), any(), any(), any()) } just Runs

        every { store.state } returns state
        every { state.session } returns null
        every { state.searchEngineSource } returns searchEngine

        val interactor = SearchInteractor(context, mockk(), store)

        interactor.onSearchTermsTapped("test")
        verify { context.openToBrowserAndLoad(
            searchTermOrURL = "test",
            newTab = true,
            from = BrowserDirection.FromSearch,
            engine = searchEngine.searchEngine,
            forceSearch = true
        ) }
    }

    @Test
    fun onSearchShortcutEngineSelected() {
        val context: HomeActivity = mockk()
        val searchEngine: SearchEngine = mockk(relaxed = true)

        val store = SearchStore(
            SearchState(
                query = "",
                showShortcutEnginePicker = false,
                searchEngineSource = SearchEngineSource.Default(mock()),
                showSuggestions = true,
                showVisitedSitesBookmarks = true,
                session = mock()
            ),
            ::searchStateReducer
        )

        every { context.metrics } returns mockk(relaxed = true)

        val interactor = SearchInteractor(context, mockk(), store)

        runBlocking {
            interactor.onSearchShortcutEngineSelected(searchEngine)
        }

        assertEquals(SearchEngineSource.Shortcut(searchEngine), store.state.searchEngineSource)
    }

    @Test
    fun onClickSearchEngineSettings() {
        val navController: NavController = mockk()
        val interactor = SearchInteractor(mockk(), navController, mockk())

        every { navController.navigate(any() as NavDirections) } just Runs

        interactor.onClickSearchEngineSettings()

        verify {
            navController.navigate(SearchFragmentDirections.actionSearchFragmentToSearchEngineFragment())
        }
    }
}
