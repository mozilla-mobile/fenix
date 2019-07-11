/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.quickactionsheet

import mozilla.components.lib.state.Action
import mozilla.components.lib.state.State
import mozilla.components.lib.state.Store

typealias QuickActionSheetStore = Store<QuickActionSheetState, QuickActionSheetAction>

data class QuickActionSheetState(
    val readable: Boolean,
    val bookmarked: Boolean,
    val readerActive: Boolean,
    val bounceNeeded: Boolean
) : State

sealed class QuickActionSheetAction : Action {
    data class BookmarkedStateChange(val bookmarked: Boolean) : QuickActionSheetAction()
    data class ReadableStateChange(val readable: Boolean) : QuickActionSheetAction()
    data class ReaderActiveStateChange(val active: Boolean) : QuickActionSheetAction()
    object BounceNeededChange : QuickActionSheetAction()
}

fun quickActionSheetStateReducer(
    state: QuickActionSheetState,
    action: QuickActionSheetAction
): QuickActionSheetState {
    return when (action) {
        is QuickActionSheetAction.BookmarkedStateChange ->
            state.copy(bookmarked = action.bookmarked)
        is QuickActionSheetAction.ReadableStateChange ->
            state.copy(readable = action.readable)
        is QuickActionSheetAction.ReaderActiveStateChange ->
            state.copy(readerActive = action.active)
        QuickActionSheetAction.BounceNeededChange ->
            state.copy(bounceNeeded = true)
    }
}
