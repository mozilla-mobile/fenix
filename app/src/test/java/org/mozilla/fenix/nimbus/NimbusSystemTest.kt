/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.nimbus

import android.content.Context
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mozilla.experiments.nimbus.NimbusInterface
import org.mozilla.fenix.experiments.maybeFetchExperiments
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.utils.Settings

class NimbusSystemTest {

    lateinit var context: Context
    lateinit var nimbus: NimbusUnderTest
    lateinit var settings: Settings

    private val lastTimeSlot = slot<Long>()

    val config = NimbusSystem(
        refreshIntervalForeground = 60,
    )

    class NimbusUnderTest(override val context: Context) : NimbusInterface {
        var fetching = false

        override fun fetchExperiments() {
            fetching = true
        }
    }

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        nimbus = NimbusUnderTest(context)

        settings = mockk(relaxed = true)
        every { context.settings() } returns settings

        every { settings.nimbusLastFetchTime = capture(lastTimeSlot) } just runs
        every { settings.nimbusLastFetchTime } returns 0L

        assertFalse(nimbus.fetching)
    }

    @Test
    fun `GIVEN a nimbus object WHEN calling maybeFetchExperiments after an interval THEN call fetchExperiments`() {
        val elapsedTime: Long = 60 * 60 * 1000 + 1
        nimbus.maybeFetchExperiments(
            context,
            config,
            elapsedTime
        )
        assertTrue(nimbus.fetching)
        assertEquals(elapsedTime, lastTimeSlot.captured)
    }

    @Test
    fun `GIVEN a nimbus object WHEN calling maybeFetchExperiments before an interval THEN do not call fetchExperiments`() {
        val elapsedTime: Long = 60 * 60 * 1000 - 1
        nimbus.maybeFetchExperiments(
            context,
            config,
            elapsedTime
        )
        assertFalse(nimbus.fetching)
    }
}
