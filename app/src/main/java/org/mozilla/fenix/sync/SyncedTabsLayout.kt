/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.sync

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.component_sync_tabs.view.*
import mozilla.components.browser.storage.sync.SyncedDeviceTabs
import mozilla.components.feature.syncedtabs.view.SyncedTabsView
import org.mozilla.fenix.R

class SyncedTabsLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), SyncedTabsView {

    override var listener: SyncedTabsView.Listener? = null

    private val adapter = SyncedTabsAdapter { listener?.onTabClicked(it) }

    init {
        inflate(getContext(), R.layout.component_sync_tabs, this)

        synced_tabs_list.layoutManager = LinearLayoutManager(context)
        synced_tabs_list.adapter = adapter

        synced_tabs_pull_to_refresh.setOnRefreshListener { listener?.onRefresh() }
    }

    override fun onError(error: SyncedTabsView.ErrorType) {
        // We may still be displaying a "loading" spinner, hide it.
        stopLoading()

        val stringResId = when (error) {
            SyncedTabsView.ErrorType.MULTIPLE_DEVICES_UNAVAILABLE -> R.string.synced_tabs_connect_another_device
            SyncedTabsView.ErrorType.SYNC_ENGINE_UNAVAILABLE -> R.string.synced_tabs_enable_tab_syncing
            SyncedTabsView.ErrorType.SYNC_UNAVAILABLE -> R.string.synced_tabs_connect_to_sync_account
            SyncedTabsView.ErrorType.SYNC_NEEDS_REAUTHENTICATION -> R.string.synced_tabs_reauth
            SyncedTabsView.ErrorType.NO_TABS_AVAILABLE -> R.string.synced_tabs_no_tabs
        }

        sync_tabs_status.text = context.getText(stringResId)

        synced_tabs_list.visibility = View.GONE
        sync_tabs_status.visibility = View.VISIBLE

        synced_tabs_pull_to_refresh.isEnabled = pullToRefreshEnableState(error)
    }

    override fun displaySyncedTabs(syncedTabs: List<SyncedDeviceTabs>) {
        synced_tabs_list.visibility = View.VISIBLE
        sync_tabs_status.visibility = View.GONE

        val allDeviceTabs = emptyList<SyncedTabsAdapter.AdapterItem>().toMutableList()

        syncedTabs.forEach { (device, tabs) ->
            if (tabs.isEmpty()) {
                return@forEach
            }

            val deviceTabs = tabs.map { SyncedTabsAdapter.AdapterItem.Tab(it) }

            allDeviceTabs += listOf(SyncedTabsAdapter.AdapterItem.Device(device)) + deviceTabs
        }

        adapter.submitList(allDeviceTabs)
    }

    override fun startLoading() {
        synced_tabs_list.visibility = View.VISIBLE
        sync_tabs_status.visibility = View.GONE

        synced_tabs_pull_to_refresh.isRefreshing = true
    }

    override fun stopLoading() {
        synced_tabs_pull_to_refresh.isRefreshing = false
    }

    companion object {
        internal fun pullToRefreshEnableState(error: SyncedTabsView.ErrorType) = when (error) {
            // Disable "pull-to-refresh" when we clearly can't sync tabs, and user needs to take an
            // action within the app.
            SyncedTabsView.ErrorType.SYNC_UNAVAILABLE,
            SyncedTabsView.ErrorType.SYNC_NEEDS_REAUTHENTICATION -> false

            // Enable "pull-to-refresh" when an external event (e.g. connecting a desktop client,
            // or enabling tabs sync, or connecting to a network) may resolve our problem.
            SyncedTabsView.ErrorType.SYNC_ENGINE_UNAVAILABLE,
            SyncedTabsView.ErrorType.MULTIPLE_DEVICES_UNAVAILABLE,
            SyncedTabsView.ErrorType.NO_TABS_AVAILABLE -> true
        }
    }
}
