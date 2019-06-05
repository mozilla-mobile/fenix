/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.forEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mozilla.components.concept.sync.AccountObserver
import mozilla.components.concept.sync.ConstellationState
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.concept.sync.Profile
import mozilla.components.concept.sync.DeviceConstellationObserver
import mozilla.components.concept.sync.SyncStatusObserver
import mozilla.components.feature.sync.getLastSynced
import mozilla.components.service.fxa.FxaException
import mozilla.components.service.fxa.FxaPanicException
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.ext.requireComponents
import kotlin.Exception
import kotlin.coroutines.CoroutineContext

class AccountSettingsFragment : PreferenceFragmentCompat(), CoroutineScope {
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job
    private lateinit var accountManager: FxaAccountManager

    // Navigate away from this fragment when we encounter auth problems or logout events.
    private val accountStateObserver = object : AccountObserver {
        override fun onAuthenticated(account: OAuthAccount) {}

        override fun onAuthenticationProblems() {
            launch {
                Navigation.findNavController(view!!).popBackStack()
            }
        }

        override fun onError(error: Exception) {}

        override fun onLoggedOut() {
            launch {
                Navigation.findNavController(view!!).popBackStack()
            }
        }

        override fun onProfileUpdated(profile: Profile) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
        (activity as AppCompatActivity).title = getString(R.string.preferences_account_settings)
        (activity as AppCompatActivity).supportActionBar?.show()
        requireComponents.analytics.metrics.track(Event.SyncAccountOpened)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        requireComponents.analytics.metrics.track(Event.SyncAccountClosed)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.account_settings_preferences, rootKey)

        accountManager = requireComponents.backgroundServices.accountManager
        accountManager.register(accountStateObserver, this, true)

        // Sign out
        val signOut = context!!.getPreferenceKey(R.string.pref_key_sign_out)
        val preferenceSignOut = findPreference<Preference>(signOut)
        preferenceSignOut?.onPreferenceClickListener = getClickListenerForSignOut()

        // Sync now
        val syncNow = context!!.getPreferenceKey(R.string.pref_key_sync_now)
        val preferenceSyncNow = findPreference<Preference>(syncNow)
        preferenceSyncNow?.let {
            preferenceSyncNow.onPreferenceClickListener = getClickListenerForSyncNow()

            // Current sync state
            updateLastSyncedTimePref(context!!, preferenceSyncNow)
            if (requireComponents.backgroundServices.syncManager.isSyncRunning()) {
                preferenceSyncNow.title = getString(R.string.sync_syncing_in_progress)
                preferenceSyncNow.isEnabled = false
            } else {
                preferenceSyncNow.isEnabled = true
            }
        }

        // Device Name
        val deviceConstellation = accountManager.authenticatedAccount()?.deviceConstellation()
        val deviceNameKey = context!!.getPreferenceKey(R.string.pref_key_sync_device_name)
        findPreference<Preference>(deviceNameKey)?.apply {
            onPreferenceChangeListener = getChangeListenerForDeviceName()
            deviceConstellation?.state()?.currentDevice?.let { device ->
                summary = device.displayName
            }
        }

        deviceConstellation?.registerDeviceObserver(deviceConstellationObserver, owner = this, autoPause = true)

        // NB: ObserverRegistry will take care of cleaning up internal references to 'observer' and
        // 'owner' when appropriate.
        requireComponents.backgroundServices.syncManager.register(syncStatusObserver, owner = this, autoPause = true)
    }

    private fun getClickListenerForSignOut(): Preference.OnPreferenceClickListener {
        return Preference.OnPreferenceClickListener {
            requireComponents.analytics.metrics.track(Event.SyncAccountSignOut)
            launch {
                accountManager.logoutAsync().await()
            }
            true
        }
    }

    private fun getClickListenerForSyncNow(): Preference.OnPreferenceClickListener {
        return Preference.OnPreferenceClickListener {
            // Trigger a sync.
            requireComponents.analytics.metrics.track(Event.SyncAccountSyncNow)
            requireComponents.backgroundServices.syncManager.syncNow()
            // Poll for device events.
            launch {
                accountManager.authenticatedAccount()
                    ?.deviceConstellation()
                    ?.refreshDeviceStateAsync()
                    ?.await()
            }
            true
        }
    }

    private fun getChangeListenerForDeviceName(): Preference.OnPreferenceChangeListener {
        return Preference.OnPreferenceChangeListener { _, newValue ->
            // Optimistically set the device name to what user requested.
            val deviceNameKey = context!!.getPreferenceKey(R.string.pref_key_sync_device_name)
            val preferenceDeviceName = findPreference<Preference>(deviceNameKey)
            preferenceDeviceName?.summary = newValue as String

            // This may fail, and we'll have a disparity in the UI until `updateDeviceName` is called.
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    accountManager.authenticatedAccount()?.let {
                        it.deviceConstellation().setDeviceNameAsync(newValue)
                    }
                } catch (e: FxaPanicException) {
                    throw e
                } catch (e: FxaException) {
                    Logger.error("Setting device name failed.", e)
                }
            }

            true
        }
    }

    private val syncStatusObserver = object : SyncStatusObserver {
        override fun onStarted() {
            CoroutineScope(Dispatchers.Main).launch {
                val pref = findPreference<Preference>(context!!.getPreferenceKey(R.string.pref_key_sync_now))
                view?.announceForAccessibility(getString(R.string.sync_syncing_in_progress))
                pref?.title = getString(R.string.sync_syncing_in_progress)
                pref?.isEnabled = false

                updateSyncingItemsPreference()
            }
        }

        // Sync stopped successfully.
        override fun onIdle() {
            CoroutineScope(Dispatchers.Main).launch {
                val pref = findPreference<Preference>(context!!.getPreferenceKey(R.string.pref_key_sync_now))
                pref?.let {
                    pref.title = getString(R.string.preferences_sync_now)
                    pref.isEnabled = true
                    updateLastSyncedTimePref(context!!, pref, failed = false)
                }
            }
        }

        // Sync stopped after encountering a problem.
        override fun onError(error: Exception?) {
            CoroutineScope(Dispatchers.Main).launch {
                val pref = findPreference<Preference>(context!!.getPreferenceKey(R.string.pref_key_sync_now))
                pref?.let {
                    pref.title = getString(R.string.preferences_sync_now)
                    pref.isEnabled = true
                    updateLastSyncedTimePref(context!!, pref, failed = true)
                }
            }
        }
    }

    private val deviceConstellationObserver = object : DeviceConstellationObserver {
        override fun onDevicesUpdate(constellation: ConstellationState) {
            val deviceNameKey = context!!.getPreferenceKey(R.string.pref_key_sync_device_name)
            val preferenceDeviceName = findPreference<Preference>(deviceNameKey)
            preferenceDeviceName?.summary = constellation.currentDevice?.displayName
        }
    }

    private fun updateSyncingItemsPreference() {
        val syncCategory = context!!.getPreferenceKey(R.string.preferences_sync_category)
        val preferencesSyncCategory = findPreference<Preference>(syncCategory) as PreferenceCategory
        val stringSet = mutableSetOf<String>()

        preferencesSyncCategory.forEach {
            (it as? CheckBoxPreference)?.let { checkboxPreference ->
                if (checkboxPreference.isChecked) {
                    stringSet.add(checkboxPreference.key)
                }
            }
        }
    }

    fun updateLastSyncedTimePref(context: Context, pref: Preference, failed: Boolean = false) {
        val lastSyncTime = getLastSynced(context)

        pref.summary = if (!failed && lastSyncTime == 0L) {
            // Never tried to sync.
            getString(R.string.sync_never_synced_summary)
        } else if (failed && lastSyncTime == 0L) {
            // Failed to sync, never succeeded before.
            getString(R.string.sync_failed_never_synced_summary)
        } else if (!failed && lastSyncTime != 0L) {
            // Successfully synced.
            getString(
                R.string.sync_last_synced_summary,
                DateUtils.getRelativeTimeSpanString(lastSyncTime)
            )
        } else {
            // Failed to sync, succeeded before.
            getString(
                R.string.sync_failed_summary,
                DateUtils.getRelativeTimeSpanString(lastSyncTime)
            )
        }
    }
}
