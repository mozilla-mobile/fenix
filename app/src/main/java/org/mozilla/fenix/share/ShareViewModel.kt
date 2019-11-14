/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.share

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.core.content.getSystemService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import mozilla.components.concept.sync.DeviceCapability
import mozilla.components.service.fxa.manager.FxaAccountManager
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.share.listadapters.AppShareOption
import org.mozilla.fenix.share.listadapters.SyncShareOption

class ShareViewModel(application: Application) : AndroidViewModel(application) {

    private val connectivityManager by lazy { application.getSystemService<ConnectivityManager>() }
    private val fxaAccountManager = application.components.backgroundServices.accountManager

    private val devicesListLiveData = MutableLiveData<List<SyncShareOption>>(emptyList())
    private val appsListLiveData = MutableLiveData<List<AppShareOption>>(emptyList())

    @VisibleForTesting
    internal val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network?) = reloadDevices()
        override fun onAvailable(network: Network?) = reloadDevices()

        private fun reloadDevices() {
            viewModelScope.launch(IO) {
                fxaAccountManager.authenticatedAccount()
                    ?.deviceConstellation()
                    ?.refreshDevicesAsync()
                    ?.await()

                val devicesShareOptions = buildDeviceList(fxaAccountManager)
                devicesListLiveData.postValue(devicesShareOptions)
            }
        }
    }

    /**
     * List of devices and sync-related share options.
     */
    val devicesList: LiveData<List<SyncShareOption>> get() = devicesListLiveData
    /**
     * List of applications that can be shared to.
     */
    val appsList: LiveData<List<AppShareOption>> get() = appsListLiveData

    /**
     * Load a list of devices and apps into [devicesList] and [appsList].
     * Should be called when a fragment is attached so the data can be fetched early.
     */
    fun loadDevicesAndApps() {
        val networkRequest = NetworkRequest.Builder().build()
        connectivityManager?.registerNetworkCallback(networkRequest, networkCallback)

        // Start preparing the data as soon as we have a valid Context
        viewModelScope.launch(IO) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val shareAppsActivities = getIntentActivities(shareIntent, getApplication())
            val apps = buildAppsList(shareAppsActivities, getApplication())
            appsListLiveData.postValue(apps)
        }

        viewModelScope.launch(IO) {
            val devices = buildDeviceList(fxaAccountManager)
            devicesListLiveData.postValue(devices)
        }
    }

    override fun onCleared() {
        connectivityManager?.unregisterNetworkCallback(networkCallback)
    }

    @WorkerThread
    private fun getIntentActivities(shareIntent: Intent, context: Context): List<ResolveInfo>? {
        return context.packageManager.queryIntentActivities(shareIntent, 0)
    }

    /**
     * Returns a list of apps that can be shared to.
     * @param intentActivities List of activities from [getIntentActivities].
     */
    @VisibleForTesting
    @WorkerThread
    internal fun buildAppsList(
        intentActivities: List<ResolveInfo>?,
        context: Context
    ): List<AppShareOption> {
        return intentActivities
            .orEmpty()
            .filter { it.activityInfo.packageName != context.packageName }
            .map { resolveInfo ->
                AppShareOption(
                    resolveInfo.loadLabel(context.packageManager).toString(),
                    resolveInfo.loadIcon(context.packageManager),
                    resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name
                )
            }
    }

    /**
     * Builds list of options to display in the top row of the share sheet.
     * This will primarily include devices that tabs can be sent to, but also options
     * for reconnecting the account or sending to all devices.
     */
    @VisibleForTesting
    @WorkerThread
    internal fun buildDeviceList(accountManager: FxaAccountManager): List<SyncShareOption> {
        val activeNetwork = connectivityManager?.activeNetworkInfo
        val account = accountManager.authenticatedAccount()

        return when {
            // No network
            activeNetwork?.isConnected != true -> listOf(SyncShareOption.Offline)
            // No account signed in
            account == null -> listOf(SyncShareOption.SignIn)
            // Account needs to be re-authenticated
            accountManager.accountNeedsReauth() -> listOf(SyncShareOption.Reconnect)
            // Signed in
            else -> {
                val shareableDevices = account.deviceConstellation().state()
                    ?.otherDevices
                    .orEmpty()
                    .filter { it.capabilities.contains(DeviceCapability.SEND_TAB) }

                val list = mutableListOf<SyncShareOption>()
                if (shareableDevices.isEmpty()) {
                    // Show add device button if there are no devices
                    list.add(SyncShareOption.AddNewDevice)
                }

                shareableDevices.mapTo(list) { SyncShareOption.SingleDevice(it) }

                if (shareableDevices.size > 1) {
                    // Show send all button if there are multiple devices
                    list.add(SyncShareOption.SendAll(shareableDevices))
                }
                list
            }
        }
    }
}
