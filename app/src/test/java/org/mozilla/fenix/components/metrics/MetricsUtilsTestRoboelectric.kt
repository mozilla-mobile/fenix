/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.metrics

import io.mockk.every
import io.mockk.mockk
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.support.test.robolectric.testContext
import mozilla.telemetry.glean.testing.GleanTestRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.GleanMetrics.Events
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner

/**
 * Just the Roboelectric tests for MetricsUtil. Splitting these files out means our other tests will run more quickly.
 * FenixRobolectricTestRunner also breaks our ability to use mockkStatic on Base64.
 */
@RunWith(FenixRobolectricTestRunner::class)
class MetricsUtilsTestRoboelectric {

    @get:Rule
    val gleanTestRule = GleanTestRule(testContext)

    @Test
    fun `WHEN recordSearchEvent is called THEN the right event is recorded`() {
        val store = BrowserStore()
        val engine: SearchEngine = mockk(relaxed = true)

        every { engine.id } returns MetricsUtilsTest.ENGINE_SOURCE_IDENTIFIER
        assertFalse(Events.performedSearch.testHasValue())

        MetricsUtils.recordSearchEvent(
            engine,
            store,
            MetricsUtils.SearchAccessPoint.SUGGESTION
        )

        assertTrue(Events.performedSearch.testHasValue())
        assertEquals(
            "shortcut.suggestion",
            Events.performedSearch.testGetValue().last().extra?.get("source")
        )

        MetricsUtils.recordSearchEvent(
            engine,
            store,
            MetricsUtils.SearchAccessPoint.ACTION
        )

        assertTrue(Events.performedSearch.testHasValue())
        assertEquals(
            "shortcut.action",
            Events.performedSearch.testGetValue().last().extra?.get("source")
        )

        MetricsUtils.recordSearchEvent(
            engine,
            store,
            MetricsUtils.SearchAccessPoint.WIDGET
        )

        assertTrue(Events.performedSearch.testHasValue())
        assertEquals(
            "shortcut.widget",
            Events.performedSearch.testGetValue().last().extra?.get("source")
        )

        MetricsUtils.recordSearchEvent(
            engine,
            store,
            MetricsUtils.SearchAccessPoint.SHORTCUT
        )

        assertTrue(Events.performedSearch.testHasValue())
        assertEquals(
            "shortcut.shortcut",
            Events.performedSearch.testGetValue().last().extra?.get("source")
        )
    }
}
