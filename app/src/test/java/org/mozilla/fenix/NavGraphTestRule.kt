/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import io.mockk.every
import io.mockk.mockkObject
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.mozilla.fenix.perf.NavGraphProvider

class NavGraphTestRule : TestWatcher() {

    override fun starting(description: Description?) {
        mockkObject(NavGraphProvider)
        every { NavGraphProvider.blockForNavGraphInflation(any()) } returns Unit
    }
}
