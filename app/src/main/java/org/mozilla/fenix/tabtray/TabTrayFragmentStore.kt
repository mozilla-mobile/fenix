package org.mozilla.fenix.tabtray

import android.content.Context
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import mozilla.components.browser.session.Session
import mozilla.components.lib.state.Action
import mozilla.components.lib.state.State
import mozilla.components.lib.state.Store
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.getColorFromAttr
import org.mozilla.fenix.ext.setToolbarColors

typealias Tab = Session

data class TabTrayFragmentState(val tabs: List<Tab>, val mode: Mode) : State {
    sealed class Mode {
        open val selectedTabs = emptySet<Tab>()
        object Normal : Mode()
        data class Editing(override val selectedTabs: Set<Tab>) : Mode()
    }
}

sealed class TabTrayFragmentAction: Action {
    data class UpdateTabs(val tabs: List<Tab>) : TabTrayFragmentAction()
    data class SelectTab(val tab: Tab) : TabTrayFragmentAction()
    data class DeselectTab(val tab: Tab) : TabTrayFragmentAction()
    object ExitEditMode: TabTrayFragmentAction()
    object EnterEditMode: TabTrayFragmentAction()
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
        is TabTrayFragmentAction.EnterEditMode -> state.copy(
            mode = TabTrayFragmentState.Mode.Editing(emptySet())
        )
        is TabTrayFragmentAction.SelectTab ->
            state.copy(
                mode = TabTrayFragmentState.Mode.Editing(state.mode.selectedTabs + action.tab)
            )
        is TabTrayFragmentAction.DeselectTab -> {
            val selected = state.mode.selectedTabs - action.tab
            state.copy(
                mode = TabTrayFragmentState.Mode.Editing(selected)
            )
        }
        is TabTrayFragmentAction.ExitEditMode -> state.copy(mode = TabTrayFragmentState.Mode.Normal)
        is TabTrayFragmentAction.UpdateTabs -> state.copy(tabs = action.tabs)
    }
}

fun TabTrayFragmentState.appBarTitle(context: Context): String {
    return when (this.mode) {
        is TabTrayFragmentState.Mode.Normal -> context.getString(R.string.tab_tray_title)
        is TabTrayFragmentState.Mode.Editing -> if (this.mode.selectedTabs.isEmpty()) {
            context.getString(R.string.tab_tray_menu_item_save_select)
        } else {
            context.getString(R.string.history_multi_select_title, this.mode.selectedTabs.size)
        }
    }
}

fun TabTrayFragmentState.appBarBackground(context: Context): Pair<Int, Int> {
    return when (mode) {
        is TabTrayFragmentState.Mode.Normal -> Pair(
            context.getColorFromAttr(R.attr.primaryText),
            context.getColorFromAttr(R.attr.foundation)
        )
        is TabTrayFragmentState.Mode.Editing -> Pair(
            ContextCompat.getColor(context, R.color.white_color),
            context.getColorFromAttr(R.attr.accentHighContrast)
        )
    }
}

fun TabTrayFragmentState.appBarShowCollectionIcon(): Boolean {
    return when (this.mode) {
        is TabTrayFragmentState.Mode.Normal -> false
        is TabTrayFragmentState.Mode.Editing -> true
    }
}

fun TabTrayFragmentState.appBarIcon(): Int {
    return when (this.mode) {
        is TabTrayFragmentState.Mode.Normal -> R.drawable.mozac_ic_back
        is TabTrayFragmentState.Mode.Editing -> R.drawable.ic_close
    }
}
