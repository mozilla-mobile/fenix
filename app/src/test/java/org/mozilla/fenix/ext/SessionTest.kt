/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.browser.session.Session
import mozilla.components.feature.media.state.MediaState
import org.mozilla.fenix.home.sessioncontrol.Tab

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class SessionTest() {

    @Test
    fun `GIVEN session WHEN converting to tab THEN send back a correctly-populated tab`() {
        val session = Session("https://www.mozilla.org/en-US/")
        val tabToCompare = Tab(session.id, session.url, session.url.urlToTrimmedHost(testContext), session.title, null, null, session.icon)
        val tabResult = session.toTab(testContext, null, null)
        assertEquals(tabToCompare, tabResult)
    }
}

