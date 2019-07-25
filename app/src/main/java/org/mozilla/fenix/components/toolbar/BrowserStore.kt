package org.mozilla.fenix.components.toolbar

import mozilla.components.lib.state.Action
import mozilla.components.lib.state.State
import mozilla.components.lib.state.Store

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

sealed class BrowserAction : Action

// TODO: Ughhh this has to be in the same file as the BrowserAction definition is that fine?
// TODO: This is because sealed classes have `private init`
/**
 * Actions to dispatch through the [QuickActionSheetStore] to modify [QuickActionSheetState] through the reducer.
 */
sealed class QuickActionSheetAction : BrowserAction() {
    data class BookmarkedStateChange(val bookmarked: Boolean) : QuickActionSheetAction()
    data class ReadableStateChange(val readable: Boolean) : QuickActionSheetAction()
    data class ReaderActiveStateChange(val active: Boolean) : QuickActionSheetAction()
    data class AppLinkStateChange(val isAppLink: Boolean) : QuickActionSheetAction()
    object BounceNeededChange : QuickActionSheetAction()
}

/**
 * Reducers for [BrowserStore].
 *
 * A reducer is a function that receives the current [BrowserState] and an [Action] and then returns a new
 * [BrowserState].
 */
fun browserStateReducer(
    state: BrowserState,
    action: BrowserAction
): BrowserState {
    return when (action) {
        is QuickActionSheetAction -> {
            QuickActionSheetStateReducer.reduce(state, action)
        }
    }
}


/**
 * Reduces [QuickActionSheetAction]s to update [BrowserState].
 */
internal object QuickActionSheetStateReducer {
    fun reduce(state: BrowserState, action: QuickActionSheetAction): BrowserState {
        return when (action) {
            is QuickActionSheetAction.BookmarkedStateChange ->
                state.copy(bookmarked = action.bookmarked)
            is QuickActionSheetAction.ReadableStateChange ->
                state.copy(readable = action.readable)
            is QuickActionSheetAction.ReaderActiveStateChange ->
                state.copy(readerActive = action.active)
            is QuickActionSheetAction.BounceNeededChange ->
                state.copy(bounceNeeded = true)
            is QuickActionSheetAction.AppLinkStateChange -> {
                state.copy(isAppLink = action.isAppLink)
            }
        }
    }
}