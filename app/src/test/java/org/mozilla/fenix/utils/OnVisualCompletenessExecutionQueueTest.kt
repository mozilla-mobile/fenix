/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
@file:Suppress("DEPRECATION")

package org.mozilla.fenix.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineStart
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.TestApplication
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = TestApplication::class)
class OnVisualCompletenessExecutionQueueTest {

    @Test
    fun `adding a paused Job to the queue succeeds`() {
        OnVisualCompletenessExecutionQueue.reset()
        OnVisualCompletenessExecutionQueue.add(CoroutineScope(Dispatchers.Default).launch(start = CoroutineStart.LAZY) {
            // This is a do-nothing job!
        })
    }

    @Test
    fun `adding an active (or possibly complete) Job to the queue fails`() {
        val invalidJob = "Job is active or complete."
        OnVisualCompletenessExecutionQueue.reset()
        try {
            // Depending on how the scheduler goes, this job could be complete by the
            // time that it is added to the queue.
            OnVisualCompletenessExecutionQueue.add(CoroutineScope(Dispatchers.Default).launch {
                // This is a do-nothing job!
            })
            Assert.fail("No exception was thrown")
        } catch (ex: VisualCompletenessExecutionQueueException) {
            assertEquals(invalidJob, ex.failure)
        }
    }

    @Test
    fun `adding a Job after queue execution fails`() {
        val invalidQueue = "Queue already executed."
        OnVisualCompletenessExecutionQueue.reset()
        OnVisualCompletenessExecutionQueue.add(CoroutineScope(Dispatchers.Default).launch(start = CoroutineStart.LAZY) {
            // This is a do-nothing job!
        })

        OnVisualCompletenessExecutionQueue.start()

        try {
            OnVisualCompletenessExecutionQueue.add(CoroutineScope(Dispatchers.Default).launch(start = CoroutineStart.LAZY) {
                // This is a do-nothing job!
            })
            Assert.fail("No exception was thrown")
        } catch (ex: VisualCompletenessExecutionQueueException) {
            assertEquals(invalidQueue, ex.failure)
        }
    }

    @Test
    fun `after queue execution fails internal structures should be empty`() {
        OnVisualCompletenessExecutionQueue.reset()
        OnVisualCompletenessExecutionQueue.add(CoroutineScope(Dispatchers.Default).launch(start = CoroutineStart.LAZY) {
            // This is a do-nothing job!
        })

        OnVisualCompletenessExecutionQueue.start()
        assertTrue(OnVisualCompletenessExecutionQueue.jobs.size == 0)
    }

    @Test
    fun `after queue execution fails hasExecuted flag should be false`() {
        OnVisualCompletenessExecutionQueue.reset()
        OnVisualCompletenessExecutionQueue.add(CoroutineScope(Dispatchers.Default).launch(start = CoroutineStart.LAZY) {
            // This is a do-nothing job!
        })

        OnVisualCompletenessExecutionQueue.start()
        assertTrue(OnVisualCompletenessExecutionQueue.hasExecuted)
    }
}
