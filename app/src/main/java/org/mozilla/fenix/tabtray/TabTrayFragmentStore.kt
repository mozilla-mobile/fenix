package org.mozilla.fenix.tabtray

import mozilla.components.browser.session.Session
import mozilla.components.lib.state.Action
import mozilla.components.lib.state.State
import mozilla.components.lib.state.Store

data class TabTrayFragmentState(val tabs: List<Session>) : State

object TabTrayFragmentAction: Action

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
    return state
}
