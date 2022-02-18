/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.viewholders

import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.lib.state.ext.observeAsComposableState
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.tabstray.NavigationInteractor
import org.mozilla.fenix.tabstray.TabsTrayStore
import org.mozilla.fenix.tabstray.TabsTrayState
import org.mozilla.fenix.tabstray.syncedtabs.SyncedTabsList
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * Temporary ViewHolder to render [SyncedTabsList] until all of the Tabs Tray is written in Compose.
 *
 * @param composeView Root ComposeView passed-in from TrayPagerAdapter.
 * @param tabsTrayStore Store used as a Composable State to listen for changes to [TabsTrayState.syncedTabs].
 * @param navigationInteractor The lambda for handling clicks on synced tabs.
 */
class SyncedTabsPageViewHolder(
    private val composeView: ComposeView,
    private val tabsTrayStore: TabsTrayStore,
    private val navigationInteractor: NavigationInteractor,
) : AbstractPageViewHolder(composeView) {

    fun bind() {
        composeView.setContent {
            val tabs = tabsTrayStore.observeAsComposableState { state -> state.syncedTabs }.value
            FirefoxTheme {
                SyncedTabsList(
                    syncedTabs = tabs ?: emptyList(),
                    taskContinuityEnabled = composeView.context.settings().enableTaskContinuityEnhancements,
                    onTabClick = navigationInteractor::onSyncedTabClicked,
                )
            }
        }
    }

    override fun bind(adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>) = Unit // no-op

    override fun detachedFromWindow() = Unit // no-op

    override fun attachedToWindow() = Unit // no-op

    companion object {
        val LAYOUT_ID = View.generateViewId()
    }
}
