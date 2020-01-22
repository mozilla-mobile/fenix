/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.exceptions

import mozilla.components.concept.engine.content.blocking.TrackingProtectionException
import mozilla.components.lib.state.Action
import mozilla.components.lib.state.State
import mozilla.components.lib.state.Store

/**
 * Class representing an exception item
 * @property url Host of the exception
 */
data class ExceptionItem(override val url: String) : TrackingProtectionException

/**
 * The [Store] for holding the [ExceptionsFragmentState] and applying [ExceptionsFragmentAction]s.
 */
class ExceptionsFragmentStore(initialState: ExceptionsFragmentState) :
    Store<ExceptionsFragmentState, ExceptionsFragmentAction>(initialState, ::exceptionsStateReducer)

/**
 * Actions to dispatch through the `ExceptionsStore` to modify `ExceptionsState` through the reducer.
 */
sealed class ExceptionsFragmentAction : Action {
    data class Change(val list: List<TrackingProtectionException>) : ExceptionsFragmentAction()
}

/**
 * The state for the Exceptions Screen
 * @property items List of exceptions to display
 */
data class ExceptionsFragmentState(val items: List<TrackingProtectionException>) : State

/**
 * The ExceptionsState Reducer.
 */
private fun exceptionsStateReducer(
    state: ExceptionsFragmentState,
    action: ExceptionsFragmentAction
): ExceptionsFragmentState {
    return when (action) {
        is ExceptionsFragmentAction.Change -> state.copy(items = action.list)
    }
}
