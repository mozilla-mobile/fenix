/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.intent

import android.content.Intent
import androidx.navigation.NavController
import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.TestApplication
import org.mozilla.fenix.browser.BrowserNavigation
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class OpenBrowserIntentProcessorTest {

    private val navController: NavController = mockk()
    private val out: Intent = mockk(relaxed = true)

    @Test
    fun `do not process blank intents`() {
        val processor = OpenBrowserIntentProcessor { null }
        processor.process(Intent(), navController, out)

        verify { navController wasNot Called }
        verify { out wasNot Called }
    }

    @Test
    fun `do not process when open extra is false`() {
        val intent = Intent().apply {
            putExtra(HomeActivity.OPEN_TO_BROWSER, false)
        }
        val processor = OpenBrowserIntentProcessor { null }
        processor.process(intent, navController, out)

        verify { navController wasNot Called }
        verify { out wasNot Called }
    }

    @Test
    fun `process when open extra is true`() {
        val intent = Intent().apply {
            putExtra(HomeActivity.OPEN_TO_BROWSER, true)
        }
        val processor = OpenBrowserIntentProcessor { "session-id" }
        mockkObject(BrowserNavigation)
        every { BrowserNavigation.openToBrowser(any(), any()) } just Runs

        processor.process(intent, navController, out)

        verify { BrowserNavigation.openToBrowser(BrowserDirection.FromGlobal, "session-id") }
        verify { navController wasNot Called }
        verify { out.putExtra(HomeActivity.OPEN_TO_BROWSER, false) }

        unmockkObject(BrowserNavigation)
    }
}
