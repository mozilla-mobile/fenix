/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabtray

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.SimpleItemAnimator
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.component_tab_tray.view.*
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.components.ui.SelectionInteractor
import org.mozilla.fenix.library.LibraryPageView
import org.mozilla.fenix.utils.allowUndo

/**
 * Interface for the TabTrayViewInteractor. This interface is implemented by objects that want
 * to respond to user interaction on the TabTrayView
 */
interface TabTrayViewInteractor : SelectionInteractor<Tab> {
    fun closeButtonTapped(tab: Tab)
    fun onPauseMediaClicked()
    fun onPlayMediaClicked()
    fun privateModeButtonTapped()
    fun normalModeButtonTapped()
    fun closeAllTabsTapped()
    fun newTabTapped()
}

/**
 * View that contains and configures the Tab Tray
 */
class TabTrayView(
    val container: ViewGroup,
    val interactor: TabTrayViewInteractor
) : LibraryPageView(container), LayoutContainer {
    private val tabTrayAdapter = TabTrayAdapter(interactor)
    private var snackbar: FenixSnackbar? = null

    val view: View = LayoutInflater.from(container.context)
        .inflate(R.layout.component_tab_tray, container, true)

    init {
        view.tab_tray_list.apply {
            adapter = tabTrayAdapter
            (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        }

        view.private_browsing_button.setOnClickListener { interactor.privateModeButtonTapped() }
        view.tab_tray_close_all.setOnClickListener { interactor.closeAllTabsTapped() }
        view.tab_tray_open_new_tab.setOnClickListener { interactor.newTabTapped() }
    }

    fun update(state: TabTrayFragmentState, browsingMode: BrowsingMode) {
        tabTrayAdapter.updateState(state)
        view.tab_tray_controls.isVisible = !state.mode.isEditing
        view.tab_tray_empty_view.isVisible = state.tabs.isEmpty()
        if (state.tabs.isEmpty()) {
            view.announceForAccessibility(view.context.getString(R.string.no_open_tabs_description))
        }

        view.private_browsing_button.setOnClickListener {
            if (browsingMode.isPrivate) {
                interactor.normalModeButtonTapped()
            } else {
                interactor.privateModeButtonTapped()
            }
        }
    }

    fun showUndoSnackbar(
        message: String,
        onCompletion: suspend () -> Unit,
        onUndo: suspend () -> Unit,
        lifecycleOwner: LifecycleOwner
    ) {
        snackbar = lifecycleOwner.lifecycleScope.allowUndo(
            view,
            message,
            view.context.getString(R.string.snackbar_deleted_undo),
            onUndo,
            onCompletion
        )
    }

    fun hideSnackbar() {
        snackbar?.dismiss()
    }
}
