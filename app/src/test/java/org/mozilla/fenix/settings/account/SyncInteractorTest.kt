/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.account

import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class SyncInteractorTest {

    private lateinit var syncInteractor: SyncInteractor
    private lateinit var syncController: SyncController

    @Before
    fun setUp() {
        syncController = mockk(relaxed = true)
        syncInteractor = SyncInteractor(syncController)
    }

    @Test
    fun onCameraPermissionsNeeded() {
        syncInteractor.onCameraPermissionsNeeded()

        verify {
            syncController.handleCameraPermissionsNeeded()
        }
    }
}
