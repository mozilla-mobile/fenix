/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.trackingprotection

import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackingProtectionPanelInteractorTest {

    private val store: TrackingProtectionStore = mockk(relaxed = true)
    private val interactor = TrackingProtectionPanelInteractor(store, mockk(), mockk())

    @Test
    fun openDetails() {
        interactor.openDetails(TrackingProtectionCategory.FINGERPRINTERS, true)
        verify {
            store.dispatch(
                TrackingProtectionAction.EnterDetailsMode(
                    TrackingProtectionCategory.FINGERPRINTERS,
                    true
                )
            )
        }
    }

    @Test
    fun selectTrackingProtectionSettings() {
        var openSettings = false
        val interactor = TrackingProtectionPanelInteractor(
            mockk(),
            mockk(),
            { openSettings = true }
        )
        interactor.selectTrackingProtectionSettings()
        assertEquals(true, openSettings)
    }

    @Test
    fun trackingProtectionToggled() {
        var trackingProtectionNewValue: Boolean? = null
        val interactor = TrackingProtectionPanelInteractor(
            mockk(),
            { trackingProtectionNewValue = it },
            mockk()
        )
        interactor.trackingProtectionToggled(true)
        assertEquals(true, trackingProtectionNewValue)
    }

    @Test
    fun onBackPressed() {
        interactor.onBackPressed()
        verify { store.dispatch(TrackingProtectionAction.ExitDetailsMode) }
    }
}
