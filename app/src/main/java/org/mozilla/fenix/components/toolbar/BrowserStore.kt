package org.mozilla.fenix.components.toolbar

import mozilla.components.lib.state.Action
import mozilla.components.lib.state.State
import mozilla.components.lib.state.Store
import org.mozilla.fenix.quickactionsheet.QuickActionSheetAction
import org.mozilla.fenix.quickactionsheet.QuickActionSheetState
import org.mozilla.fenix.quickactionsheet.QuickActionSheetStore

class BrowserStore(initialState: BrowserState) :
    Store<BrowserState, BrowserAction>(initialState, ::browserStateReducer)

/**
 * The state for the Browser Screen
 * @property readable Whether or not the current session can display a reader view
 * @property bookmarked Whether or not the current session is already bookmarked
 * @property readerActive Whether or not the current session is in reader mode
 * @property bounceNeeded Whether or not the quick action sheet should bounce
 */
data class BrowserState(
    val readable: Boolean,
    val bookmarked: Boolean,
    val readerActive: Boolean,
    val bounceNeeded: Boolean,
    val isAppLink: Boolean
) : State

/**
 * Actions to dispatch through the [QuickActionSheetStore] to modify [QuickActionSheetState] through the reducer.
 */

// TODO: Again these names feel weird when pulled into an abstraction...
sealed class BrowserAction : Action {
    data class BookmarkedStateChange(val bookmarked: Boolean) : BrowserAction()
    data class ReadableStateChange(val readable: Boolean) : BrowserAction()
    data class ReaderActiveStateChange(val active: Boolean) : BrowserAction()
    data class AppLinkStateChange(val isAppLink: Boolean) : BrowserAction()
    object BounceNeededChange : BrowserAction()
}

/**
 * Reduces [QuickActionSheetAction]s to update [QuickActionSheetState].
 */
fun browserStateReducer(
    state: BrowserState,
    action: BrowserAction
): BrowserState {
    return when (action) {
        is BrowserAction.BookmarkedStateChange ->
            state.copy(bookmarked = action.bookmarked)
        is BrowserAction.ReadableStateChange ->
            state.copy(readable = action.readable)
        is BrowserAction.ReaderActiveStateChange ->
            state.copy(readerActive = action.active)
        is BrowserAction.BounceNeededChange ->
            state.copy(bounceNeeded = true)
        is BrowserAction.AppLinkStateChange -> {
            state.copy(isAppLink = action.isAppLink)
        }
    }
}
