/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.perf

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verifySequence
import mozilla.components.service.glean.private.TimespanMetricType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class StartupTimelineTest {

    @MockK(relaxed = true) lateinit var metric: TimespanMetricType

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `WHEN measure is called THEN the metric's start and stop are called in sequence with no other metric calls`() {
        StartupTimeline.measure(metric) { /* do nothing */ }
        verifySequence {
            metric.start()
            metric.stop()
        }
    }

    @Test
    fun `WHEN measure is called but the measured function throws an exception THEN start and cancel are called in in sequence with no other metric calls`() {
        try {
            StartupTimeline.measure(metric) { throw IllegalStateException() }
        } catch (e: IllegalStateException) {
            // Do nothing: unit tests should test non-overlapping functionality and we're not
            // testing the exception in this test.
        }

        verifySequence {
            metric.start()
            metric.cancel()
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `WHEN measure is called but the measured function throws an exception THEN the exception is propagated`() {
        StartupTimeline.measure(metric) { throw IllegalStateException() }
    }

    @Test
    fun `WHEN measure is called THEN the return value of the measuredFunction is returned`() {
        val expected = 4
        val actual = StartupTimeline.measure(metric) { expected }
        assertEquals(expected, actual)
    }
}
