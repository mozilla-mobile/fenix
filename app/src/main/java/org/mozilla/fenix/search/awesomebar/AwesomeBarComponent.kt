/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.search.awesomebar

import android.view.ViewGroup
import mozilla.components.browser.search.SearchEngine
import org.mozilla.fenix.mvi.ViewState
import org.mozilla.fenix.mvi.Change
import org.mozilla.fenix.mvi.Action
import org.mozilla.fenix.mvi.ActionBusFactory
import org.mozilla.fenix.mvi.Reducer
import org.mozilla.fenix.mvi.UIComponent
import org.mozilla.fenix.mvi.UIComponentViewModelBase
import org.mozilla.fenix.mvi.UIComponentViewModelProvider

data class AwesomeBarState(
    val query: String,
    val showShortcutEnginePicker: Boolean,
    val suggestionEngine: SearchEngine? = null
) : ViewState

sealed class AwesomeBarAction : Action {
    data class URLTapped(val url: String) : AwesomeBarAction()
    data class SearchTermsTapped(val searchTerms: String, val engine: SearchEngine? = null) : AwesomeBarAction()
    data class SearchShortcutEngineSelected(val engine: SearchEngine) : AwesomeBarAction()
}

sealed class AwesomeBarChange : Change {
    data class SearchShortcutEngineSelected(val engine: SearchEngine) : AwesomeBarChange()
    data class SearchShortcutEnginePicker(val show: Boolean) : AwesomeBarChange()
    data class UpdateQuery(val query: String) : AwesomeBarChange()
}

class AwesomeBarComponent(
    private val container: ViewGroup,
    bus: ActionBusFactory,
    viewModelProvider: UIComponentViewModelProvider<AwesomeBarState, AwesomeBarChange>
) : UIComponent<AwesomeBarState, AwesomeBarAction, AwesomeBarChange>(
    bus.getManagedEmitter(AwesomeBarAction::class.java),
    bus.getSafeManagedObservable(AwesomeBarChange::class.java),
    viewModelProvider
) {
    override fun initView() = AwesomeBarUIView(container, actionEmitter, changesObservable)

    init {
        bind()
    }
}

class AwesomeBarViewModel(
    initialState: AwesomeBarState
) : UIComponentViewModelBase<AwesomeBarState, AwesomeBarChange>(initialState, reducer) {
    companion object {
        val reducer: Reducer<AwesomeBarState, AwesomeBarChange> = { state, change ->
            when (change) {
                is AwesomeBarChange.SearchShortcutEngineSelected ->
                    state.copy(suggestionEngine = change.engine, showShortcutEnginePicker = false)
                is AwesomeBarChange.SearchShortcutEnginePicker ->
                    state.copy(showShortcutEnginePicker = change.show)
                is AwesomeBarChange.UpdateQuery -> state.copy(query = change.query)
            }
        }
    }
}
