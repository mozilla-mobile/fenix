/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.bookmarks

import android.view.ViewGroup
import mozilla.components.concept.storage.BookmarkNode
import org.mozilla.fenix.mvi.Action
import org.mozilla.fenix.mvi.ActionBusFactory
import org.mozilla.fenix.mvi.Change
import org.mozilla.fenix.mvi.Reducer
import org.mozilla.fenix.mvi.UIComponent
import org.mozilla.fenix.mvi.UIView
import org.mozilla.fenix.mvi.ViewState
import org.mozilla.fenix.test.Mockable

@Mockable
class BookmarkComponent(
    private val container: ViewGroup,
    bus: ActionBusFactory,
    override var initialState: BookmarkState =
        BookmarkState(null, BookmarkState.Mode.Normal)
) :
    UIComponent<BookmarkState, BookmarkAction, BookmarkChange>(
        bus.getManagedEmitter(BookmarkAction::class.java),
        bus.getSafeManagedObservable(BookmarkChange::class.java)
    ) {

    override val reducer: Reducer<BookmarkState, BookmarkChange> = { state, change ->
        when (change) {
            is BookmarkChange.Change -> {
                val mode =
                    if (state.mode is BookmarkState.Mode.Selecting) {
                        BookmarkState.Mode.Selecting(state.mode.selectedItems.filter {
                            it in change.tree
                        }.toSet())
                    } else state.mode
                state.copy(tree = change.tree, mode = mode)
            }
            is BookmarkChange.IsSelected -> {
                val selectedItems = if (state.mode is BookmarkState.Mode.Selecting) {
                    state.mode.selectedItems + change.newlySelectedItem
                } else setOf(change.newlySelectedItem)
                state.copy(mode = BookmarkState.Mode.Selecting(selectedItems))
            }
            is BookmarkChange.IsDeselected -> {
                val selectedItems = if (state.mode is BookmarkState.Mode.Selecting) {
                    state.mode.selectedItems - change.newlyDeselectedItem
                } else setOf()
                val mode = if (selectedItems.isEmpty()) BookmarkState.Mode.Normal else BookmarkState.Mode.Selecting(
                    selectedItems
                )
                state.copy(mode = mode)
            }
        }
    }

    override fun initView(): UIView<BookmarkState, BookmarkAction, BookmarkChange> =
        BookmarkUIView(container, actionEmitter, changesObservable)

    init {
        render(reducer)
    }
}

data class BookmarkState(val tree: BookmarkNode?, val mode: BookmarkState.Mode) : ViewState {
    sealed class Mode {
        object Normal : Mode()
        data class Selecting(val selectedItems: Set<BookmarkNode>) : Mode()
    }
}

sealed class BookmarkAction : Action {
    data class Open(val item: BookmarkNode) : BookmarkAction()
    data class Expand(val folder: BookmarkNode) : BookmarkAction()
    data class Edit(val item: BookmarkNode) : BookmarkAction()
    data class Copy(val item: BookmarkNode) : BookmarkAction()
    data class Share(val item: BookmarkNode) : BookmarkAction()
    data class OpenInNewTab(val item: BookmarkNode) : BookmarkAction()
    data class OpenInPrivateTab(val item: BookmarkNode) : BookmarkAction()
    data class Select(val item: BookmarkNode) : BookmarkAction()
    data class Deselect(val item: BookmarkNode) : BookmarkAction()
    data class Delete(val item: BookmarkNode) : BookmarkAction()
    object BackPressed : BookmarkAction()
    object ModeChanged : BookmarkAction()
}

sealed class BookmarkChange : Change {
    data class Change(val tree: BookmarkNode) : BookmarkChange()
    data class IsSelected(val newlySelectedItem: BookmarkNode) : BookmarkChange()
    data class IsDeselected(val newlyDeselectedItem: BookmarkNode) : BookmarkChange()
}

operator fun BookmarkNode.contains(item: BookmarkNode): Boolean {
    return children?.contains(item) ?: false
}
