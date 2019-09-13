/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test
import org.mozilla.fenix.settings.account.AccountSettingsFragmentAction
import org.mozilla.fenix.settings.account.AccountSettingsFragmentState
import org.mozilla.fenix.settings.account.AccountSettingsFragmentStore
import org.mozilla.fenix.settings.account.LastSyncTime

class AccountSettingsFragmentStoreTest {

    private val initialState = AccountSettingsFragmentState()
    private val store = AccountSettingsFragmentStore(initialState)
    private val duration = 1L
    private val deviceName = "testing"

    @Test
    fun syncFailed() = runBlocking {
        store.dispatch(AccountSettingsFragmentAction.SyncFailed(duration)).join()
        assertNotSame(initialState, store.state)
        assertEquals(LastSyncTime.Failed(duration), store.state.lastSyncedDate)
    }

    @Test
    fun syncEnded() = runBlocking {
        store.dispatch(AccountSettingsFragmentAction.SyncEnded(duration)).join()
        assertNotSame(initialState, store.state)
        assertEquals(LastSyncTime.Success(duration), store.state.lastSyncedDate)
    }

    @Test
    fun signOut() = runBlocking {
        store.dispatch(AccountSettingsFragmentAction.UpdateDeviceName(deviceName)).join()
        assertNotSame(initialState, store.state)
        assertEquals(deviceName, store.state.deviceName)
    }
}
