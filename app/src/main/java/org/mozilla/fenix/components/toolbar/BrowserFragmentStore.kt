/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("unused", "UNUSED_PARAMETER")

package org.mozilla.fenix.components.toolbar

import mozilla.components.lib.state.Action
import mozilla.components.lib.state.State
import mozilla.components.lib.state.Store

// TODO ... remove file? Find things that should live here and refactor them in?
class BrowserFragmentStore(initialState: BrowserFragmentState) :
    Store<BrowserFragmentState, BrowserFragmentAction>(initialState, ::browserStateReducer)

/**
 * The state for the Browser Screen
 * @property quickActionSheetState: state of the quick action sheet
 */
class BrowserFragmentState : State

sealed class BrowserFragmentAction : Action

/**
 * Reducers for [BrowserFragmentStore].
 *
 * A top level reducer that receives the current [BrowserFragmentState] and an [Action] and then
 * delegates to the proper child
 */
private fun browserStateReducer(
    state: BrowserFragmentState,
    action: BrowserFragmentAction
): BrowserFragmentState {
    return when {
        else -> BrowserFragmentState()
    }
}

/**
 * Reduces [QuickActionSheetAction]s to update [BrowserFragmentState].
 */
internal object QuickActionSheetStateReducer {
    fun reduce(state: BrowserFragmentState): BrowserFragmentState {
        return when {
            else -> state
        }
    }
}
