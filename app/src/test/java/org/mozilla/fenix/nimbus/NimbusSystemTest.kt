/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.nimbus

import android.content.Context
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    // By default this comes from the generated Nimbus features.
    val config = NimbusSystem(
        refreshIntervalForeground = 60, /* minutes */
    )

    class NimbusUnderTest(override val context: Context) : NimbusInterface {
        var isFetching = false

        override fun fetchExperiments() {
            isFetching = true
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

        assertFalse(nimbus.isFetching)
    }

    @Test
    fun `GIVEN a nimbus object WHEN calling maybeFetchExperiments after an interval THEN call fetchExperiments`() {
        val elapsedTime: Long = Settings.ONE_HOUR_MS + 1
        nimbus.maybeFetchExperiments(
            context,
            config,
            elapsedTime,
        )
        assertTrue(nimbus.isFetching)
        assertEquals(elapsedTime, lastTimeSlot.captured)
    }

    @Test
    fun `GIVEN a nimbus object WHEN calling maybeFetchExperiments at exactly an interval THEN call fetchExperiments`() {
        val elapsedTime: Long = Settings.ONE_HOUR_MS
        nimbus.maybeFetchExperiments(
            context,
            config,
            elapsedTime,
        )
        assertTrue(nimbus.isFetching)
        assertEquals(elapsedTime, lastTimeSlot.captured)
    }

    @Test
    fun `GIVEN a nimbus object WHEN calling maybeFetchExperiments before an interval THEN do not call fetchExperiments`() {
        val elapsedTime: Long = Settings.ONE_HOUR_MS - 1
        nimbus.maybeFetchExperiments(
            context,
            config,
            elapsedTime,
        )
        assertFalse(nimbus.isFetching)
    }

    @Test
    fun `GIVEN a nimbus object WHEN calling maybeFetchExperiments at without an elapsedTime THEN call fetchExperiments`() {
        // since elapsedTime = currentTimeMillis
        nimbus.maybeFetchExperiments(
            context,
            config,
        )
        assertTrue(nimbus.isFetching)
    }
}
