package org.mozilla.fenix.tabtray

import mozilla.components.browser.session.Session
import mozilla.components.lib.state.Action
import mozilla.components.lib.state.State
import mozilla.components.lib.state.Store

typealias Tab = Session

data class TabTrayFragmentState(val tabs: List<Tab>) : State

sealed class TabTrayFragmentAction: Action {
    data class UpdateTabs(val tabs: List<Tab>) : TabTrayFragmentAction()
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
    }
}
