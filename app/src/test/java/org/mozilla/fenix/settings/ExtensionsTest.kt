/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.content.SharedPreferences
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner

@RunWith(FenixRobolectricTestRunner::class)
class ExtensionsTest {

    @Test
    fun testSetOnPreferenceChangeListener() {
        val preference = Preference(testContext)
        var called = false
        preference.setOnPreferenceChangeListener<Boolean> { _, _ ->
            called = true
            true
        }

        preference.callChangeListener("hello")
        assertFalse(called)

        preference.callChangeListener(false)
        assertTrue(called)
    }

    @Test
    fun testAddMetricsPreferenceListener() {
        val fragment = mockk<PreferenceFragmentCompat>()
        val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
        val metrics = mockk<MetricController>()

        every { fragment.requireContext() } returns testContext
        every { fragment.requireContext().resources } returns testContext.resources
        every { fragment.requireComponents.analytics.metrics } returns metrics
        every { fragment.preferenceManager.sharedPreferences } returns sharedPreferences
        every { metrics.track(any()) } just Runs

        val slot = slot<OnSharedPreferenceChangeListener>()
        every { fragment.lifecycle.addObserver(capture(slot)) } just Runs

        fragment.addMetricsPreferenceListener()
        val key = "pref_key_search_bookmarks"

        every { sharedPreferences.getBoolean(key, false) } returns false
        slot.captured.onSharedPreferenceChanged(sharedPreferences, key)
        verify {
            metrics.track(
                Event.PreferenceToggled(key, false, testContext.resources)
            )
        }
        clearMocks(metrics)

        every { sharedPreferences.getBoolean(key, false) } throws IllegalArgumentException()
        slot.captured.onSharedPreferenceChanged(sharedPreferences, key)
        verify { metrics wasNot Called }

        every { sharedPreferences.getBoolean(key, false) } throws ClassCastException()
        slot.captured.onSharedPreferenceChanged(sharedPreferences, key)
        verify { metrics wasNot Called }
    }
}
