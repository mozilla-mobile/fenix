/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray

import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mozilla.components.concept.base.profiler.Profiler
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.service.fxa.sync.SyncReason
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.tabtray.TabTrayDialogFragmentDirections

interface TabsTrayController {

    /**
     * Called when user clicks the new tab button.
     */
    fun onNewTabTapped(isPrivate: Boolean)

    /**
     * Starts user account tab syncing.
     * */
    fun onSyncStarted()
}

class DefaultTabsTrayController(
    private val store: TabsTrayStore,
    private val browsingModeManager: BrowsingModeManager,
    private val navController: NavController,
    private val profiler: Profiler?,
    private val dismissTabTray: () -> Unit,
    private val metrics: MetricController,
    private val ioScope: CoroutineScope,
    private val accountManager: FxaAccountManager
) : TabsTrayController {

    override fun onNewTabTapped(isPrivate: Boolean) {
        val startTime = profiler?.getProfilerTime()
        browsingModeManager.mode = BrowsingMode.fromBoolean(isPrivate)
        navController.navigate(TabTrayDialogFragmentDirections.actionGlobalHome(focusOnAddressBar = true))
        dismissTabTray()
        profiler?.addMarker(
            "DefaultTabTrayController.onNewTabTapped",
            startTime
        )
    }

    override fun onSyncStarted() {
        ioScope.launch {
            metrics.track(Event.SyncAccountSyncNow)
            // Trigger a sync.
            accountManager.syncNow(SyncReason.User)
            // Poll for device events & update devices.
            accountManager.authenticatedAccount()
                ?.deviceConstellation()?.run {
                    refreshDevices()
                    pollForCommands()
                }
        }.invokeOnCompletion {
            store.dispatch(TabsTrayAction.SyncCompleted)
        }
    }
}
