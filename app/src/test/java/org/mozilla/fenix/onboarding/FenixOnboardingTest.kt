/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.onboarding.FenixOnboarding.Companion.CURRENT_ONBOARDING_VERSION
import org.mozilla.fenix.onboarding.FenixOnboarding.Companion.LAST_VERSION_ONBOARDING_KEY

class FenixOnboardingTest {

    private lateinit var onboarding: FenixOnboarding
    private lateinit var preferences: SharedPreferences
    private lateinit var preferencesEditor: SharedPreferences.Editor
    private lateinit var metrics: MetricController

    @Before
    fun setup() {
        preferences = mockk()
        preferencesEditor = mockk(relaxed = true)
        metrics = mockk()
        val context = mockk<Context>()
        every { preferences.edit() } returns preferencesEditor
        every { metrics.track(any()) } returns Unit
        every { context.components.analytics.metrics } returns metrics
        every { context.getSharedPreferences(any(), MODE_PRIVATE) } returns preferences

        onboarding = FenixOnboarding(context)
    }

    @Test
    fun testUserHasBeenOnboarded() {
        every {
            preferences.getInt(LAST_VERSION_ONBOARDING_KEY, any())
        } returns 0
        assertFalse(onboarding.userHasBeenOnboarded())

        every {
            preferences.getInt(LAST_VERSION_ONBOARDING_KEY, any())
        } returns CURRENT_ONBOARDING_VERSION
        assertTrue(onboarding.userHasBeenOnboarded())
    }

    @Test
    fun testFinish() {
        onboarding.finish()
        verify { preferencesEditor.putInt(LAST_VERSION_ONBOARDING_KEY, CURRENT_ONBOARDING_VERSION) }
        verify { metrics.track(Event.DismissedOnboarding) }
    }
}
