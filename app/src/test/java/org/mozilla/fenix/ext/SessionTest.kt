package org.mozilla.fenix.ext

import androidx.navigation.fragment.NavHostFragment
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.spyk
import mozilla.components.support.test.robolectric.testContext
import org.mozilla.fenix.TestApplication
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.assertEquals
import mozilla.components.browser.session.Session
import org.mozilla.fenix.home.sessioncontrol.Tab
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)

class SessionTest {
    val id: String = "c3ba04cb-e5ee-4768-b40c-159595205554"
    val url: String = "https://www.mozilla.org/en-US/"
    val urlTrimmedToHost:String = "mozilla.org"

    @Test
    fun `GIVEN session WHEN converting to tab THEN send back a correctly-populated tab`() {
        mockkStatic(UUID::class)
        every { UUID.randomUUID().toString() } returns "7777"
        val session = spyk(Session("https://www.mozilla.org/en-US/"))
        val tabToCompare = Tab("7777", url, urlTrimmedToHost, "", null, null, null)
        val tabResult = session.toTab(testContext, null, null)
        assertEquals(tabToCompare, tabResult)
    }
}