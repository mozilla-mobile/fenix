/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mozilla.components.concept.sync.SyncResult
import mozilla.components.concept.sync.StoreSyncStatus
import mozilla.components.concept.sync.SyncStatus
import mozilla.components.concept.sync.SyncStatusObserver
import mozilla.components.feature.sync.getLastSynced
import mozilla.components.service.glean.Glean
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.ext.requireComponents
import java.lang.Exception
import kotlin.coroutines.CoroutineContext
import org.mozilla.fenix.GleanMetrics.BookmarksSync
import org.mozilla.fenix.GleanMetrics.HistorySync
import org.mozilla.fenix.GleanMetrics.Pings
import java.util.Date

class AccountSettingsFragment : PreferenceFragmentCompat(), CoroutineScope {
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    companion object {
        private const val LOGTAG = "AccountSettingsFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
        (activity as AppCompatActivity).title = getString(R.string.preferences_account_settings)
        (activity as AppCompatActivity).supportActionBar?.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.account_settings_preferences, rootKey)

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
                preferenceSyncNow.title = getString(R.string.sync_syncing)
                preferenceSyncNow.isEnabled = false
            } else {
                preferenceSyncNow.isEnabled = true
            }
        }

        // NB: ObserverRegistry will take care of cleaning up internal references to 'observer' and
        // 'owner' when appropriate.
        requireComponents.backgroundServices.syncManager.register(syncStatusObserver, owner = this, autoPause = true)
    }

    private fun getClickListenerForSignOut(): Preference.OnPreferenceClickListener {
        return Preference.OnPreferenceClickListener {
            launch {
                requireComponents.backgroundServices.accountManager.logoutAsync().await()
                Navigation.findNavController(view!!).popBackStack()
            }
            true
        }
    }

    private fun getClickListenerForSyncNow(): Preference.OnPreferenceClickListener {
        return Preference.OnPreferenceClickListener {
            requireComponents.backgroundServices.syncManager.syncNow()
            true
        }
    }

    private val syncStatusObserver = object : SyncStatusObserver {
        override fun onStarted() {
            CoroutineScope(Dispatchers.Main).launch {
                val pref = findPreference<Preference>(context!!.getPreferenceKey(R.string.pref_key_sync_now))
                view?.announceForAccessibility(getString(R.string.sync_syncing))
                pref?.title = getString(R.string.sync_syncing)
                pref?.isEnabled = false
            }
        }

        // Sync stopped successfully.
        override fun onIdle(result: SyncResult) {
            CoroutineScope(Dispatchers.Main).launch {
                val pref = findPreference<Preference>(context!!.getPreferenceKey(R.string.pref_key_sync_now))
                pref?.let {
                    pref.title = getString(R.string.preferences_sync_now)
                    pref.isEnabled = true
                    updateLastSyncedTimePref(context!!, pref, failed = false)
                }

                Log.i(LOGTAG, "Sync finished; recording telemetry")
                for ((_, status) in result) {
                    status.stats?.let {
                        it.syncs.forEach {
                            it.engines.forEach {
                                when (it.name) {
                                    "history" -> {
                                        Log.i(LOGTAG, "Got history sync")
                                        HistorySync.apply {
                                            startedAt.set(Date(it.at.toLong()))
                                            // Glean doesn't support recording arbitrary durations in
                                            // timespans, so we record absolute start and end times instead.
                                            finishedAt.set(Date((it.at + it.took).toLong()))
                                            incoming["applied"].set(it.incoming?.applied ?: 0)
                                            val failedToApply = it.incoming?.let {
                                                it.failed + it.newFailed
                                            } ?: 0;
                                            incoming["failed_to_apply"].set(failedToApply)
                                            incoming["reconciled"].set(it.incoming?.reconciled ?: 0)
                                            val (uploaded, failedToUpload) = it.outgoing.fold(Pair(0, 0)) { totals, batch ->
                                                val (uploaded, failedToUpload) = totals
                                                Pair(uploaded + batch.sent, failedToUpload + batch.failed)
                                            }
                                            outgoing["uploaded"].set(uploaded)
                                            outgoing["failed_to_upload"].set(failedToUpload)
                                            outgoingBatches.set(it.outgoing.size)
                                            // TODO: We don't currently record the failure reason, because an
                                            // error while syncing causes us to discard all telemetry.
                                        }
                                        Log.i(LOGTAG, "Sending history ping")
                                        Pings.historySync.send()
                                    }
                                    "bookmarks" -> {
                                        BookmarksSync.apply {
                                            startedAt.set(Date(it.at.toLong()))
                                            finishedAt.set(Date((it.at + it.took).toLong()))
                                            incoming["applied"].set(it.incoming?.applied ?: 0)
                                            val failedToApply = it.incoming?.let {
                                                it.failed + it.newFailed
                                            } ?: 0;
                                            incoming["failed_to_apply"].set(failedToApply)
                                            incoming["reconciled"].set(it.incoming?.reconciled ?: 0)
                                            val (uploaded, failedToUpload) = it.outgoing.fold(Pair(0, 0)) { totals, batch ->
                                                val (uploaded, failedToUpload) = totals
                                                Pair(uploaded + batch.sent, failedToUpload + batch.failed)
                                            }
                                            outgoing["uploaded"].set(uploaded)
                                            outgoing["failed_to_upload"].set(failedToUpload)
                                            outgoingBatches.set(it.outgoing.size)
                                        }
                                        Log.i(LOGTAG, "Sending bookmarks ping")
                                        Pings.bookmarksSync.send()
                                    }
                                    "logins" -> {
                                        // TODO: Collect stats for logins.
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sync stopped after encountering a problem.
        override fun onError(result: SyncResult) {
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
