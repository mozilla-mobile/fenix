/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins

import mozilla.components.concept.storage.Login
import mozilla.components.lib.state.Action
import mozilla.components.lib.state.State
import mozilla.components.lib.state.Store

/**
 * The [Store] for holding the [SavedLoginsFragmentState] and applying [SavedLoginsFragmentAction]s.
 */
class LoginsFragmentStore(initialState: LoginsListState) :
    Store<LoginsListState, LoginsAction>(
        initialState,
        ::savedLoginsStateReducer
    )

/**
 * Actions to dispatch through the `SavedLoginsStore` to modify `LoginsListState` through the reducer.
 */
sealed class LoginsAction : Action {
    data class FilterLogins(val newText: String?) : LoginsAction()
    data class UpdateLoginsList(val list: List<Login>) : LoginsAction()
}

/**
 * The state for the Saved Logins Screen
 * @property loginList Source of truth for local list of logins
 * @property loginList Filterable list of logins to display
 */
data class LoginsListState(
    val isLoading: Boolean = false,
    val loginList: List<Login>,
    val filteredItems: List<Login>
) : State

/**
 * Handles changes in the saved logins list, including updates and filtering.
 */
private fun savedLoginsStateReducer(
    state: LoginsListState,
    action: LoginsAction
): LoginsListState {
    return when (action) {
        is LoginsAction.UpdateLoginsList -> state.copy(
            isLoading = false,
            loginList = action.list,
            filteredItems = action.list
        )
        is LoginsAction.FilterLogins -> {
            if (action.newText.isNullOrBlank()) {
                state.copy(
                    isLoading = false,
                    filteredItems = state.loginList)
            } else {
                state.copy(
                    isLoading = false,
                    filteredItems = state.loginList.filter { it.origin.contains(action.newText) })
            }
        }
    }
}
