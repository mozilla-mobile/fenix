/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import io.mockk.every
import io.mockk.mockk
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SessionManagerTest {

    private val sessionManager: SessionManager = mockk()

    private val sessions = listOf(
        Session("https://example.com", private = false),
        Session("https://mozilla.org", private = true),
        Session("https://github.com", private = false)
    )

    @Before
    fun setUp() {
        every { sessionManager.sessions } returns sessions
    }

    @Test
    fun `returns all normal sessions`() {
        assertEquals(
            listOf(sessions[0], sessions[2]),
            sessionManager.sessionsOfType(private = false).toList()
        )
    }

    @Test
    fun `returns all private sessions`() {
        assertEquals(
            listOf(sessions[1]),
            sessionManager.sessionsOfType(private = true).toList()
        )
    }
}
