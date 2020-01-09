/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import android.content.Intent
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.utils.toSafeIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.HomeActivity.Companion.PRIVATE_BROWSING_MODE
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.settings
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class HomeActivityTest {

    @Test
    fun getIntentSource() {
        val activity = HomeActivity()

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
        val activity = HomeActivity()

        testContext.settings().lastKnownMode = BrowsingMode.Private

        assertEquals(testContext.settings().lastKnownMode, activity.getModeFromIntentOrLastKnown(null))
    }

    @Test
    fun `getModeFromIntentOrLastKnown returns mode from intent when set`() {
        val activity = HomeActivity()

        testContext.settings().lastKnownMode = BrowsingMode.Normal

        val intent = Intent()
        intent.putExtra(PRIVATE_BROWSING_MODE, true)

        assertNotEquals(testContext.settings().lastKnownMode, activity.getModeFromIntentOrLastKnown(intent))
        assertEquals(BrowsingMode.Private, activity.getModeFromIntentOrLastKnown(intent))
    }
}
