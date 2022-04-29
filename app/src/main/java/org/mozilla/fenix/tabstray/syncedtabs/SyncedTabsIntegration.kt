/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.syncedtabs

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import mozilla.components.browser.storage.sync.SyncedDeviceTabs
import mozilla.components.feature.syncedtabs.SyncedTabsFeature
import mozilla.components.feature.syncedtabs.storage.SyncedTabsStorage
import mozilla.components.feature.syncedtabs.view.SyncedTabsView
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.base.observer.ObserverRegistry
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.tabstray.FloatingActionButtonBinding
import org.mozilla.fenix.tabstray.TabsTrayAction
import org.mozilla.fenix.tabstray.TabsTrayStore
import org.mozilla.fenix.tabstray.ext.toComposeList
import org.mozilla.fenix.tabstray.ext.toSyncedTabsListItem

/**
 * TabsTrayFragment delegate to handle all layout updates needed to display synced tabs and any errors.
 *
 * @param store [TabsTrayStore]
 * @param context Fragment context.
 * @param navController The controller used to handle any navigation necessary for error scenarios.
 * @param storage An instance of [SyncedTabsStorage] used for retrieving synced tabs.
 * @param accountManager An instance of [FxaAccountManager] used for synced tabs authentication.
 * @param lifecycleOwner View lifecycle owner used to determine when to cancel UI jobs.
 */
class SyncedTabsIntegration(
    private val store: TabsTrayStore,
    private val context: Context,
    private val navController: NavController,
    storage: SyncedTabsStorage,
    accountManager: FxaAccountManager,
    lifecycleOwner: LifecycleOwner,
) : LifecycleAwareFeature,
    SyncedTabsView,
    Observable<SyncedTabsView.Listener> by ObserverRegistry() {

    private val syncedTabsFeature by lazy {
        SyncedTabsFeature(
            context = context,
            storage = storage,
            accountManager = accountManager,
            view = this,
            lifecycleOwner = lifecycleOwner,
            onTabClicked = {
                // We can ignore this callback here because we're not connecting the Compose UI
                // back to the feature.
            }
        )
    }

    private val syncButtonBinding by lazy {
        SyncButtonBinding(store) { listener?.onRefresh() }
    }

    override var listener: SyncedTabsView.Listener? = null

    override fun start() {
        syncedTabsFeature.start()
        syncButtonBinding.start()
    }

    override fun stop() {
        syncedTabsFeature.stop()
        syncButtonBinding.stop()
    }

    override fun onError(error: SyncedTabsView.ErrorType) {
        // We may still be displaying a "loading" spinner, hide it.
        stopLoading()

        store.dispatch(TabsTrayAction.UpdateSyncedTabs(listOf(error.toSyncedTabsListItem(context, navController))))
    }

    /**
     * Do nothing; the UI is handled with [FloatingActionButtonBinding].
     */
    override fun startLoading() = Unit

    override fun stopLoading() {
        store.dispatch(TabsTrayAction.SyncCompleted)
    }

    override fun displaySyncedTabs(syncedTabs: List<SyncedDeviceTabs>) {
        store.dispatch(
            TabsTrayAction.UpdateSyncedTabs(
                syncedTabs.toComposeList(
                    context.settings().enableTaskContinuityEnhancements
                )
            )
        )
    }
}
