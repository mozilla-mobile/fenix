package org.mozilla.fenix

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner

@RunWith(FenixRobolectricTestRunner::class)
class RunBlockingCounterTest {

    @Test
    fun `GIVEN we call runBlockingCounter THEN the count should increase`() {
        assertEquals(0, RunblockingCounter.runBlockingCount)
        runBlockingCounter {}
        assertEquals(1, RunblockingCounter.runBlockingCount)
    }
}