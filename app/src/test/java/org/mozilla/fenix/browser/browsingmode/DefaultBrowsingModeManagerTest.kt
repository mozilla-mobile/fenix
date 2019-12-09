/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser.browsingmode

import androidx.lifecycle.Observer
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.clearMocks
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.TestApplication
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = TestApplication::class)
class DefaultBrowsingModeManagerTest {

    private val initMode = BrowsingMode.Normal

    @Test
    fun `AWHEN mode is updated THEN callback is invoked`() {
        DefaultBrowsingModeManager.initMode(initMode)
        val mockedObserver: Observer<BrowsingMode> = spyk(Observer { })
        DefaultBrowsingModeManager.currentMode.observeForever(mockedObserver)

        // Clear invocations because liveData might have been called before
        clearMocks(mockedObserver)

        DefaultBrowsingModeManager.mode = BrowsingMode.Private
        DefaultBrowsingModeManager.mode = BrowsingMode.Private
        DefaultBrowsingModeManager.mode = BrowsingMode.Private

        verify(exactly = 3) { mockedObserver.onChanged(any()) }

        DefaultBrowsingModeManager.mode = BrowsingMode.Normal
        DefaultBrowsingModeManager.mode = BrowsingMode.Normal

        verify(exactly = 5) { mockedObserver.onChanged(any()) }
    }

    @Test
    fun `WHEN mode is updated THEN it should be returned from get`() {
        DefaultBrowsingModeManager.initMode(initMode)

        assertEquals(BrowsingMode.Normal, DefaultBrowsingModeManager.mode)

        DefaultBrowsingModeManager.mode = BrowsingMode.Private
        assertEquals(BrowsingMode.Private, DefaultBrowsingModeManager.mode)

        DefaultBrowsingModeManager.mode = BrowsingMode.Normal
        assertEquals(BrowsingMode.Normal, DefaultBrowsingModeManager.mode)
    }
}
