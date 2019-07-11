/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.search

import mozilla.components.browser.search.SearchEngine
import mozilla.components.browser.session.Session
import mozilla.components.lib.state.Action
import mozilla.components.lib.state.State
import mozilla.components.lib.state.Store

/**
 * An alias to make it easier to work with `Store<SearchState, SearchAction>`
 */
typealias SearchStore = Store<SearchState, SearchAction>

/**
 * Wraps a `SearchEngine` to give consumers the context that it was selected as a shortcut
 */
sealed class SearchEngineSource {
    abstract val searchEngine: SearchEngine

    data class Default(override val searchEngine: SearchEngine) : SearchEngineSource()
    data class Shortcut(override val searchEngine: SearchEngine) : SearchEngineSource()
}

/**
 * The state for the Search Screen
 */
data class SearchState(
    val query: String = "",
    val searchTerms: String = "",
    val showShortcutEnginePicker: Boolean = false,
    val searchEngineSource: SearchEngineSource,
    val showSuggestions: Boolean,
    val showVisitedSitesBookmarks: Boolean,
    val session: Session?
) : State

/**
 * Actions to dispatch through the `SearchStore` to modify `SearchState` through the reducer.
 */
sealed class SearchAction : Action {
    data class SearchShortcutEngineSelected(val engine: SearchEngine) : SearchAction()
    data class SearchShortcutEnginePicker(val show: Boolean) : SearchAction()
    data class UpdateQuery(val query: String) : SearchAction()
}

/**
 * The SearchState Reducer.
 */
fun searchStateReducer(state: SearchState, action: SearchAction): SearchState {
    return when (action) {
        is SearchAction.SearchShortcutEngineSelected ->
            state.copy(
                searchEngineSource = SearchEngineSource.Shortcut(action.engine),
                showShortcutEnginePicker = false
            )
        is SearchAction.SearchShortcutEnginePicker ->
            state.copy(showShortcutEnginePicker = action.show)
        is SearchAction.UpdateQuery ->
            state.copy(query = action.query)
    }
}
