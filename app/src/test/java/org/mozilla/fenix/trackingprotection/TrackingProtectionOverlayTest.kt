/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.trackingprotection

import android.content.Context
import android.view.View
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import mozilla.components.browser.session.Session
import mozilla.components.support.test.robolectric.testContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.utils.Settings

@RunWith(FenixRobolectricTestRunner::class)
class TrackingProtectionOverlayTest {

    private lateinit var context: Context
    @MockK(relaxed = true) private lateinit var settings: Settings
    @MockK(relaxed = true) private lateinit var metrics: MetricController
    @MockK(relaxed = true) private lateinit var toolbar: View
    @MockK(relaxed = true) private lateinit var icon: View
    @MockK(relaxed = true) private lateinit var session: Session
    @MockK(relaxed = true) private lateinit var overlay: TrackingProtectionOverlay

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        context = spyk(testContext)

        overlay = TrackingProtectionOverlay(context, settings, metrics) { toolbar }
        every { toolbar.findViewById<View>(R.id.mozac_browser_toolbar_tracking_protection_indicator) } returns icon
    }

    @Test
    fun `no-op when loading`() {
        every { settings.shouldShowTrackingProtectionCfr } returns true
        every { session.trackerBlockingEnabled } returns true
        every { session.trackersBlocked } returns listOf(mockk())

        overlay.onLoadingStateChanged(session, loading = true)
        verify(exactly = 0) { settings.incrementTrackingProtectionOnboardingCount() }
    }

    @Test
    fun `no-op when should not show onboarding`() {
        every { settings.shouldShowTrackingProtectionCfr } returns false

        overlay.onLoadingStateChanged(session, loading = false)
        verify(exactly = 0) { settings.incrementTrackingProtectionOnboardingCount() }
    }

    @Test
    fun `no-op when tracking protection disabled`() {
        every { settings.shouldShowTrackingProtectionCfr } returns true
        every { session.trackerBlockingEnabled } returns false

        overlay.onLoadingStateChanged(session, loading = false)
        verify(exactly = 0) { settings.incrementTrackingProtectionOnboardingCount() }
    }

    @Test
    fun `no-op when no trackers blocked`() {
        every { settings.shouldShowTrackingProtectionCfr } returns true
        every { session.trackerBlockingEnabled } returns true
        every { session.trackersBlocked } returns emptyList()

        overlay.onLoadingStateChanged(session, loading = false)
        verify(exactly = 0) { settings.incrementTrackingProtectionOnboardingCount() }
    }

    @Test
    fun `show onboarding when trackers are blocked`() {
        every { toolbar.hasWindowFocus() } returns true
        every { settings.shouldShowTrackingProtectionCfr } returns true
        every { session.trackerBlockingEnabled } returns true
        every { session.trackersBlocked } returns listOf(mockk())

        overlay.onLoadingStateChanged(session, loading = false)
        verify { settings.incrementTrackingProtectionOnboardingCount() }
    }

    @Test
    fun `no-op when toolbar doesn't have focus`() {
        every { toolbar.hasWindowFocus() } returns false
        every { settings.shouldShowTrackingProtectionCfr } returns true
        every { session.trackerBlockingEnabled } returns true
        every { session.trackersBlocked } returns listOf(mockk())

        overlay.onLoadingStateChanged(session, loading = false)
        verify(exactly = 0) { settings.incrementTrackingProtectionOnboardingCount() }
    }
}
