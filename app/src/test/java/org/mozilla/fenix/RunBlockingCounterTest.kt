/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mozilla.fenix.perf.RunBlockingCounter
import org.mozilla.fenix.perf.runBlockingIncrement

class RunBlockingCounterTest {

    @Test
    fun `GIVEN we call runBlockingCounter THEN the count should increase`() {
        assertEquals(0L, RunBlockingCounter.count)
        runBlockingIncrement {}
        assertEquals(1, RunBlockingCounter.count)
    }
}
