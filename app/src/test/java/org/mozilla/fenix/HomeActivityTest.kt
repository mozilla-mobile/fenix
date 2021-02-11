/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import mozilla.components.service.glean.testing.GleanTestRule
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.utils.toSafeIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.GleanMetrics.PerfStartup
import org.mozilla.fenix.HomeActivity.Companion.PRIVATE_BROWSING_MODE
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.utils.Settings
import org.robolectric.Robolectric

@RunWith(FenixRobolectricTestRunner::class)
class HomeActivityTest {

    @get:Rule val gleanTestRule = GleanTestRule(ApplicationProvider.getApplicationContext())

    private lateinit var activity: HomeActivity

    @Before
    fun setup() {
        activity = spyk(HomeActivity())
    }

    @Test
    fun getIntentSource() {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }.toSafeIntent()
        assertEquals(Event.OpenedApp.Source.APP_ICON, activity.getIntentSource(launcherIntent))

        val viewIntent = Intent(Intent.ACTION_VIEW).toSafeIntent()
        assertEquals(Event.OpenedApp.Source.LINK, activity.getIntentSource(viewIntent))

        val otherIntent = Intent().toSafeIntent()
        assertNull(activity.getIntentSource(otherIntent))
    }

    @Test
    fun `getModeFromIntentOrLastKnown returns mode from settings when intent does not set`() {
        every { activity.applicationContext } returns testContext
        testContext.settings().lastKnownMode = BrowsingMode.Private

        assertEquals(testContext.settings().lastKnownMode, activity.getModeFromIntentOrLastKnown(null))
    }

    @Test
    fun `getModeFromIntentOrLastKnown returns mode from intent when set`() {
        testContext.settings().lastKnownMode = BrowsingMode.Normal

        val intent = Intent()
        intent.putExtra(PRIVATE_BROWSING_MODE, true)

        assertNotEquals(testContext.settings().lastKnownMode, activity.getModeFromIntentOrLastKnown(intent))
        assertEquals(BrowsingMode.Private, activity.getModeFromIntentOrLastKnown(intent))
    }

    @Test
    fun `isActivityColdStarted returns true for null savedInstanceState and not launched from history`() {
        assertTrue(activity.isActivityColdStarted(Intent(), null))
    }

    @Test
    fun `isActivityColdStarted returns false for valid savedInstanceState and not launched from history`() {
        assertFalse(activity.isActivityColdStarted(Intent(), Bundle()))
    }

    @Test
    fun `isActivityColdStarted returns false for null savedInstanceState and launched from history`() {
        val startingIntent = Intent().apply {
            flags = flags or Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
        }

        assertFalse(activity.isActivityColdStarted(startingIntent, null))
    }

    @Test
    fun `navigateToBrowserOnColdStart in normal mode navigates to browser`() {
        val browsingModeManager: BrowsingModeManager = mockk()
        every { browsingModeManager.mode } returns BrowsingMode.Normal

        val settings: Settings = mockk()
        every { settings.shouldReturnToBrowser } returns true
        every { activity.components.settings.shouldReturnToBrowser } returns true
        every { activity.openToBrowser(any(), any()) } returns Unit

        activity.browsingModeManager = browsingModeManager
        activity.navigateToBrowserOnColdStart()

        verify(exactly = 1) { activity.openToBrowser(BrowserDirection.FromGlobal, null) }
    }

    @Test
    fun `navigateToBrowserOnColdStart in private mode does not navigate to browser`() {
        val browsingModeManager: BrowsingModeManager = mockk()
        every { browsingModeManager.mode } returns BrowsingMode.Private

        val settings: Settings = mockk()
        every { settings.shouldReturnToBrowser } returns true
        every { activity.components.settings.shouldReturnToBrowser } returns true
        every { activity.openToBrowser(any(), any()) } returns Unit

        activity.browsingModeManager = browsingModeManager
        activity.navigateToBrowserOnColdStart()

        verify(exactly = 0) { activity.openToBrowser(BrowserDirection.FromGlobal, null) }
    }

    @Test
    fun `isActivityColdStarted returns false for null savedInstanceState and not launched from history`() {
        val startingIntent = Intent().apply {
            flags = flags or Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
        }

        assertFalse(activity.isActivityColdStarted(startingIntent, Bundle()))
    }

    @Test
    fun `WHEN onCreate is called THEN the duration is measured`() {
        assertFalse(PerfStartup.homeActivityOnCreate.testHasValue()) // sanity check.

        // For some reason, the androidx replacement for this method, ActivityScenario, fails so we
        // use the old Robolectric version. Perhaps it's because it forces the Activity to the
        // RESUMED state (unlike Robolectric where we can get to CREATED) so not enough code is
        // mocked for that to work.
        //
        // There are various exceptions thrown on background threads when this test runs but it
        // doesn't seem to impact correctness so we ignore them.
        Robolectric.buildActivity(HomeActivity::class.java)
            .create()
        assertTrue(PerfStartup.homeActivityOnCreate.testHasValue())
    }
}
