package org.mozilla.fenix.tabtray

import mozilla.components.browser.session.Session
import mozilla.components.lib.state.Action
import mozilla.components.lib.state.State
import mozilla.components.lib.state.Store

typealias Tab = Session

data class TabTrayFragmentState(val tabs: List<Tab>, val selectedTabs: Set<Tab>) : State

sealed class TabTrayFragmentAction: Action {
    data class UpdateTabs(val tabs: List<Tab>) : TabTrayFragmentAction()
    data class SelectTab(val tab: Tab) : TabTrayFragmentAction()
    data class DeselectTab(val tab: Tab) : TabTrayFragmentAction()
}

/**
 * The [Store] for holding the [TabTrayFragmentState] and applying [TabTrayFragmentAction]s.
 */
class TabTrayFragmentStore(initialState: TabTrayFragmentState) :
    Store<TabTrayFragmentState, TabTrayFragmentAction>(initialState, ::tabTrayStateReducer)


/**
 * The TabTrayState Reducer.
 */
private fun tabTrayStateReducer(
    state: TabTrayFragmentState,
    action: TabTrayFragmentAction
): TabTrayFragmentState {
    return when (action) {
        is TabTrayFragmentAction.UpdateTabs -> state.copy(tabs = action.tabs)
        is TabTrayFragmentAction.SelectTab -> state.copy(
            selectedTabs = state.selectedTabs + action.tab
        )
        is TabTrayFragmentAction.DeselectTab -> state.copy(
            selectedTabs = state.selectedTabs - action.tab
        )
    }
}
