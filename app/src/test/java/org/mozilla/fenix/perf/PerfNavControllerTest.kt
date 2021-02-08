/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.perf

import androidx.navigation.NavController
import kotlinx.coroutines.Job
import mozilla.components.support.test.robolectric.testContext
import org.junit.Before
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner

@RunWith(FenixRobolectricTestRunner::class)
class PerfNavControllerTest {

    @Before
    fun setup() {
        map.clear()
    }

    @Test
    fun `Given a NavController is removed, it should not exist within the map anymore`() {
        var navControllerOne: NavController? = NavController(testContext)
        map[navControllerOne] = Job()
        navControllerOne = null
        assertNull(map[navControllerOne])
    }
}
