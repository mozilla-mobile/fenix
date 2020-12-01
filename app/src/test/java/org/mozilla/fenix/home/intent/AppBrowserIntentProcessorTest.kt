/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.intent

import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner

@RunWith(FenixRobolectricTestRunner::class)
class AppBrowserIntentProcessorTest {

    private lateinit var activity: HomeActivity
    private lateinit var navController: NavController
    private lateinit var out: Intent
    private lateinit var processor: AppBrowserIntentProcessor
    private lateinit var mockSelector: Intent

    @Before
    fun setup() {
        activity = mockk(relaxed = true)
        every { activity.components.intentProcessors.intentProcessor.process(any()) } returns true
        every { activity.components.intentProcessors.privateIntentProcessor.process(any()) } returns true
        every { activity.settings().openLinksInAPrivateTab } returns false
        navController = mockk(relaxed = true)
        out = mockk()
        processor = AppBrowserIntentProcessor(activity)
        mockSelector = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_BROWSER)
        }
    }

    @Test
    fun `do not process blank intents`() {
        Assert.assertFalse(processor.process(Intent(), navController, out))

        verify { activity wasNot Called }
        verify { navController wasNot Called }
        verify { out wasNot Called }
    }

    @Test
    fun `do not process when selector is null`() {
        val intent = Intent().apply {
            selector = null
        }
        Assert.assertFalse(processor.process(intent, navController, out))

        verify { activity wasNot Called }
        verify { navController wasNot Called }
        verify { out wasNot Called }
    }

    @Test
    fun `do not process when data is null`() {
        val intent = Intent().apply {
            selector = mockSelector
            data = null
        }
        Assert.assertFalse(processor.process(intent, navController, out))

        verify { activity wasNot Called }
        verify { navController wasNot Called }
        verify { out wasNot Called }
    }

    @Test
    fun `do not process when data is empty`() {
        val intent = Intent().apply {
            selector = mockSelector
            data = Uri.parse("")
        }
        Assert.assertFalse(processor.process(intent, navController, out))

        verify { activity wasNot Called }
        verify { navController wasNot Called }
        verify { out wasNot Called }
    }

    @Test
    fun `process app browser intents`() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            selector = mockSelector
            data = Uri.parse("https://www.mozilla.org/en-US/firefox/")
        }
        Assert.assertTrue(processor.process(intent, navController, out))

        verify { activity.components.intentProcessors.intentProcessor }
        verify { activity.openToBrowser(BrowserDirection.FromGlobal) }
        verify { navController wasNot Called }
        verify { out wasNot Called }
    }

    @Test
    fun `process app browser intents private`() {
        every { activity.settings().openLinksInAPrivateTab } returns true

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            selector = mockSelector
            data = Uri.parse("https://www.mozilla.org/en-US/firefox/")
        }
        Assert.assertTrue(processor.process(intent, navController, out))

        verify { activity.components.intentProcessors.privateIntentProcessor }
        verify { activity.openToBrowser(BrowserDirection.FromGlobal) }
        verify { navController wasNot Called }
        verify { out wasNot Called }
    }
}
