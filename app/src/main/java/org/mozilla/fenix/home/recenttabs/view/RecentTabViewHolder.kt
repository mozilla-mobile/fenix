/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.recenttabs.view

import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.LifecycleOwner
import mozilla.components.lib.state.ext.observeAsComposableState
import org.mozilla.fenix.R
import org.mozilla.fenix.components.components
import org.mozilla.fenix.compose.ComposeViewHolder
import org.mozilla.fenix.home.recenttabs.interactor.RecentTabInteractor

/**
 * View holder for a recent tab item.
 *
 * @param composeView [ComposeView] which will be populated with Jetpack Compose UI content.
 * @param recentTabInteractor [RecentTabInteractor] which will have delegated to all user recent
 * tab interactions.
 * @param recentSyncedTabInteractor [RecentSyncedTabInteractor] which will have delegated to all user
 * recent synced tab interactions.
 */
class RecentTabViewHolder(
    composeView: ComposeView,
    viewLifecycleOwner: LifecycleOwner,
    private val recentTabInteractor: RecentTabInteractor,
) : ComposeViewHolder(composeView, viewLifecycleOwner) {

    init {
        val horizontalPadding =
            composeView.resources.getDimensionPixelSize(R.dimen.home_item_horizontal_margin)
        composeView.setPadding(horizontalPadding, 0, horizontalPadding, 0)
    }

    companion object {
        val LAYOUT_ID = View.generateViewId()
    }

    @Composable
    override fun Content() {
        val recentTabs = components.appStore.observeAsComposableState { state -> state.recentTabs }

        Column {
            RecentTabs(
                recentTabs = recentTabs.value ?: emptyList(),
                onRecentTabClick = { recentTabInteractor.onRecentTabClicked(it) },
                menuItems = listOf(
                    RecentTabMenuItem(
                        title = stringResource(id = R.string.recent_tab_menu_item_remove),
                        onClick = { tab -> recentTabInteractor.onRemoveRecentTab(tab) }
                    )
                )
            )
        }
    }
}
