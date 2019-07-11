package org.mozilla.fenix.search

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import mozilla.components.support.test.mock
import org.junit.Test

class SearchInteractorTest {
    @Test
    fun onUrlCommitted() {
    }

    @Test
    fun onEditingCanceled() {
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

        val interactor = SearchInteractor(mock(), mock(), store)

        runBlocking {
            interactor.onTextChanged("test")
        }

        assertEquals("test", store.state.query)
    }

    @Test
    fun onUrlTapped() {
    }

    @Test
    fun onSearchTermsTapped() {
    }

    @Test
    fun onSearchShortcutEngineSelected() {
    }

    @Test
    fun onClickSearchEngineSettings() {
    }
}