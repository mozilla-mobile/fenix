/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.fenix.mvi.ActionBusFactory
import org.mozilla.fenix.mvi.UIComponent
import org.mozilla.fenix.mvi.UIComponentViewModelProvider
import mozilla.components.feature.tab.collections.TabCollection as ACTabCollection

class SessionControlComponent(
    private val container: ViewGroup,
    private val interactor: SessionControlInteractor,
    bus: ActionBusFactory,
    viewModelProvider: UIComponentViewModelProvider<SessionControlState, SessionControlChange>
) :
    UIComponent<SessionControlState, SessionControlAction, SessionControlChange>(
        bus.getManagedEmitter(SessionControlAction::class.java),
        bus.getSafeManagedObservable(SessionControlChange::class.java),
        viewModelProvider
    ) {

    override fun initView() = SessionControlUIView(container, interactor, actionEmitter, changesObservable)

    val view: RecyclerView
        get() = uiView.view as RecyclerView

    init {
        bind()
    }
}

typealias TabCollection = ACTabCollection
