/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.recentsyncedtabs

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mozilla.components.browser.storage.sync.Tab
import mozilla.components.concept.storage.HistoryStorage
import mozilla.components.concept.sync.Device
import mozilla.components.concept.sync.DeviceType
import mozilla.components.feature.syncedtabs.storage.SyncedTabsStorage
import mozilla.components.lib.state.ext.flow
import mozilla.components.service.fxa.SyncEngine
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.service.fxa.manager.SyncEnginesStorage
import mozilla.components.service.fxa.manager.ext.withConstellation
import mozilla.components.service.fxa.store.SyncStatus
import mozilla.components.service.fxa.store.SyncStore
import mozilla.components.service.fxa.sync.SyncReason
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.ktx.kotlin.tryGetHostFromUrl
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged
import mozilla.telemetry.glean.GleanTimerId
import org.mozilla.fenix.GleanMetrics.RecentSyncedTabs
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import java.util.concurrent.TimeUnit

/**
 * Delegate to handle layout updates and dispatch actions related to the recent synced tab.
 *
 * @property appStore Store to dispatch actions to when synced tabs are updated or errors encountered.
 * @property syncStore Store to observe for changes to Sync and account status.
 * @property storage Storage layer for synced tabs.
 * @property accountManager Account manager to initiate Syncs and refresh devices.
 * @property historyStorage Storage for searching history for preview image URLs matching synced tab.
 * @property coroutineScope The scope to collect Sync state Flow updates in.
 */
@Suppress("LongParameterList")
class RecentSyncedTabFeature(
    private val context: Context,
    private val appStore: AppStore,
    private val syncStore: SyncStore,
    private val storage: SyncedTabsStorage,
    private val accountManager: FxaAccountManager,
    private val historyStorage: HistoryStorage,
    private val coroutineScope: CoroutineScope,
) : LifecycleAwareFeature {

    private var syncStartId: GleanTimerId? = null
    private var lastSyncedTabs: List<RecentSyncedTab>? = null

    override fun start() {
        collectAccountUpdates()
        collectStatusUpdates()
    }

    override fun stop() = Unit

    private fun collectAccountUpdates() {
        syncStore.flow()
            .ifChanged { state ->
                state.account != null
            }.onEach { state ->
                if (state.account != null) {
                    dispatchLoading()
                    // Sync tabs storage will fail to retrieve tabs aren't refreshed, as that action
                    // is what populates the device constellation state
                    accountManager.withConstellation { refreshDevices() }
                    accountManager.syncNow(
                        reason = SyncReason.User,
                        customEngineSubset = listOf(SyncEngine.Tabs),
                    )
                }
            }.launchIn(coroutineScope)
    }

    private fun collectStatusUpdates() {
        syncStore.flow()
            .ifChanged { state ->
                state.status
            }.onEach { state ->
                when (state.status) {
                    SyncStatus.Idle -> dispatchSyncedTabs()
                    SyncStatus.Error -> onError()
                    SyncStatus.LoggedOut -> appStore.dispatch(
                        AppAction.RecentSyncedTabStateChange(RecentSyncedTabState.None),
                    )
                    else -> Unit
                }
            }.launchIn(coroutineScope)
    }

    private fun dispatchLoading() {
        syncStartId?.let { RecentSyncedTabs.recentSyncedTabTimeToLoad.cancel(it) }
        syncStartId = RecentSyncedTabs.recentSyncedTabTimeToLoad.start()
        if (appStore.state.recentSyncedTabState == RecentSyncedTabState.None) {
            appStore.dispatch(AppAction.RecentSyncedTabStateChange(RecentSyncedTabState.Loading))
        }
    }

    private suspend fun dispatchSyncedTabs() {
        if (!isSyncedTabsEngineEnabled()) {
            appStore.dispatch(
                AppAction.RecentSyncedTabStateChange(RecentSyncedTabState.None),
            )

            return
        }

        val syncedTabs = storage.getSyncedDeviceTabs()
            .filterNot { it.device.isCurrentDevice || it.tabs.isEmpty() }
            .flatMap {
                it.tabs.map { tab ->
                    SyncedDeviceTab(it.device, tab)
                }
            }
            .ifEmpty { return }
            // We want to get the last device used based on the most recent accessed tab,
            // as described here: https://github.com/mozilla-mobile/fenix/issues/26398
            .sortedByDescending { deviceTab -> deviceTab.tab.lastUsed }
            .take(MAX_RECENT_SYNCED_TABS)
            .map { deviceTab ->
                val activeTabEntry = deviceTab.tab.active()

                val currentTime = System.currentTimeMillis()
                val maxAgeInMs = TimeUnit.DAYS.toMillis(DAYS_HISTORY_FOR_PREVIEW_IMAGE)
                val history = historyStorage.getDetailedVisits(
                    start = currentTime - maxAgeInMs,
                    end = currentTime,
                )

                // Searching history entries for any that share a top level domain and have a
                // preview image URL available casts a wider net for finding a suitable image.
                val previewImageUrl = history.find { entry ->
                    entry.url.contains(activeTabEntry.url.tryGetHostFromUrl()) && entry.previewImageUrl != null
                }?.previewImageUrl

                RecentSyncedTab(
                    deviceDisplayName = deviceTab.device.displayName,
                    deviceType = deviceTab.device.deviceType,
                    title = activeTabEntry.title,
                    url = activeTabEntry.url,
                    previewImageUrl = previewImageUrl,
                )
            }
        if (syncedTabs.isEmpty()) {
            appStore.dispatch(
                AppAction.RecentSyncedTabStateChange(RecentSyncedTabState.None),
            )
        } else {
            recordMetrics(syncedTabs.first(), lastSyncedTabs?.first(), syncStartId)
            appStore.dispatch(
                AppAction.RecentSyncedTabStateChange(RecentSyncedTabState.Success(syncedTabs)),
            )
            lastSyncedTabs = syncedTabs
        }
    }

    private fun onError() {
        if (appStore.state.recentSyncedTabState == RecentSyncedTabState.Loading) {
            appStore.dispatch(AppAction.RecentSyncedTabStateChange(RecentSyncedTabState.None))
        }
    }

    private fun recordMetrics(
        tab: RecentSyncedTab,
        lastSyncedTab: RecentSyncedTab?,
        syncStartId: GleanTimerId?,
    ) {
        RecentSyncedTabs.recentSyncedTabShown[tab.deviceType.name.lowercase()].add()
        syncStartId?.let { RecentSyncedTabs.recentSyncedTabTimeToLoad.stopAndAccumulate(it) }
        if (tab == lastSyncedTab) {
            RecentSyncedTabs.latestSyncedTabIsStale.add()
        }
    }

    private fun isSyncedTabsEngineEnabled(): Boolean {
        return SyncEnginesStorage(context).getStatus()[SyncEngine.Tabs] ?: true
    }

    companion object {
        /**
         * The number of days to search history for a preview image URL to display for a synced
         * tab.
         */

        const val DAYS_HISTORY_FOR_PREVIEW_IMAGE = 3L

        /**
         * Number of recent synced tabs we want to keep in the success state.
         */
        const val MAX_RECENT_SYNCED_TABS = 8
    }
}

/**
 * The state of the recent synced tab.
 */
sealed class RecentSyncedTabState {
    /**
     * There is no synced tab, or a user is not authenticated.
     */
    object None : RecentSyncedTabState()

    /**
     * A user is authenticated and the sync is running.
     */
    object Loading : RecentSyncedTabState()

    /**
     * A user is authenticated and most recent synced tabs have been found.
     */
    data class Success(val tabs: List<RecentSyncedTab>) : RecentSyncedTabState()
}

/**
 * A tab that was recently viewed on a synced device.
 *
 * @param deviceDisplayName The device the tab was viewed on.
 * @param title The title of the tab.
 * @param url The url of the tab.
 * @param previewImageUrl The url used to retrieve the preview image of the tab.
 */
data class RecentSyncedTab(
    val deviceDisplayName: String,
    val deviceType: DeviceType,
    val title: String,
    val url: String,
    val previewImageUrl: String?,
)

/**
 * Class representing a tab from a synced device.
 *
 * @param device The synced [Device].
 * @param tab The tab from the synced device.
 */
private data class SyncedDeviceTab(
    val device: Device,
    val tab: Tab,
)
