/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.intent

import android.content.Intent
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
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
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.browser.BrowserNavigation
import org.mozilla.fenix.browser.DirectionsProvider
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.browser.browsingmode.DefaultBrowsingModeManager
import org.mozilla.fenix.components.UseCases
import org.mozilla.fenix.widget.VoiceSearchActivity.Companion.SPEECH_PROCESSING
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class SpeechProcessingIntentProcessorTest {

    private val activity: HomeActivity = mockk(relaxed = true)
    private val navController: NavController = mockk(relaxed = true)
    private val out: Intent = mockk(relaxed = true)
    private val metrics: MetricController = mockk(relaxed = true)
    private var useCases: UseCases = mockk(relaxed = true)
    private var navHost: NavHostFragment = mockk(relaxed = true)
    private var createSessionObserver: () -> Unit = mockk(relaxed = true)
    private var directionsProvider: DirectionsProvider = mockk(relaxed = true)

    @Test
    fun `do not process blank intents`() {
        val processor = SpeechProcessingIntentProcessor(activity, metrics)
        processor.process(Intent(), navController, out)

        verify { activity wasNot Called }
        verify { navController wasNot Called }
        verify { out wasNot Called }
        verify { metrics wasNot Called }
    }

    @Test
    fun `do not process when open extra is false`() {
        val intent = Intent().apply {
            putExtra(HomeActivity.OPEN_TO_BROWSER_AND_LOAD, false)
        }
        val processor = SpeechProcessingIntentProcessor(activity, metrics)
        processor.process(intent, navController, out)

        verify { activity wasNot Called }
        verify { navController wasNot Called }
        verify { out wasNot Called }
        verify { metrics wasNot Called }
    }

    @Test
    fun `process when open extra is true`() {
        val intent = Intent().apply {
            putExtra(HomeActivity.OPEN_TO_BROWSER_AND_LOAD, true)
        }
        val processor = SpeechProcessingIntentProcessor(activity, metrics)
        mockkObject(BrowserNavigation)
        BrowserNavigation.init(navHost, useCases, directionsProvider, createSessionObserver)
        DefaultBrowsingModeManager.initMode(BrowsingMode.Normal)
        every { createSessionObserver.invoke() } just Runs
        every { BrowserNavigation.openToBrowserAndLoad(any(), any(), any(), any()) } just Runs
        every { activity.components.search.provider.getDefaultEngine(activity) } returns mockk(relaxed = true)

        processor.process(intent, navController, out)

        verify {
            BrowserNavigation.openToBrowserAndLoad(
                searchTermOrURL = "",
                newTab = true,
                from = BrowserDirection.FromGlobal,
                forceSearch = true
            )
        }
        verify { navController wasNot Called }
        verify { out.putExtra(HomeActivity.OPEN_TO_BROWSER_AND_LOAD, false) }

        unmockkObject(BrowserNavigation)
    }

    @Test
    fun `reads the speech processing extra`() {
        val intent = Intent().apply {
            putExtra(HomeActivity.OPEN_TO_BROWSER_AND_LOAD, true)
            putExtra(SPEECH_PROCESSING, "hello world")
        }
        val processor = SpeechProcessingIntentProcessor(activity, metrics)
        mockkObject(BrowserNavigation)
        BrowserNavigation.init(navHost, useCases, directionsProvider, createSessionObserver)
        DefaultBrowsingModeManager.initMode(BrowsingMode.Normal)
        every { createSessionObserver.invoke() } just Runs
        every { BrowserNavigation.openToBrowserAndLoad(any(), any(), any(), any()) } just Runs
        every { activity.components.search.provider.getDefaultEngine(activity) } returns mockk(relaxed = true)

        processor.process(intent, mockk(), mockk(relaxed = true))

        verify {
            BrowserNavigation.openToBrowserAndLoad(
                searchTermOrURL = "hello world",
                newTab = true,
                from = BrowserDirection.FromGlobal,
                forceSearch = true
            )
        }

        unmockkObject(BrowserNavigation)
    }
}
