/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabtray

import android.content.Context
import android.graphics.Bitmap
import mozilla.components.browser.session.Session
import mozilla.components.feature.media.state.MediaState
import mozilla.components.lib.publicsuffixlist.PublicSuffixList
import mozilla.components.lib.state.Action
import mozilla.components.lib.state.State
import mozilla.components.lib.state.Store
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.toShortUrl

data class Tab(
    val sessionId: String,
    val url: String,
    val hostname: String,
    val title: String,
    val selected: Boolean,
    var mediaState: MediaState,
    val icon: Bitmap?
)

fun Session.toTab(context: Context, selected: Boolean, mediaState: MediaState): Tab =
    this.toTab(context.components.publicSuffixList, selected, mediaState)

fun Session.toTab(publicSuffixList: PublicSuffixList, selected: Boolean, mediaState: MediaState): Tab {
    return Tab(
        sessionId = this.id,
        url = this.url,
        hostname = this.url.toShortUrl(publicSuffixList),
        title = this.title,
        selected = selected,
        mediaState = mediaState,
        icon = this.icon
    )
}

data class TabTrayFragmentState(val tabs: List<Tab>) : State

sealed class TabTrayFragmentAction : Action {
    data class UpdateTabs(val tabs: List<Tab>) : TabTrayFragmentAction()
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
        is TabTrayFragmentAction.UpdateTabs -> state.copy(tabs = action.tabs)
    }
}

fun TabTrayFragmentState.appBarIcon(): Int = R.drawable.mozac_ic_back
