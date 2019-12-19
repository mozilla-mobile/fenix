/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabtray

import io.mockk.every
import io.mockk.mockk
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.feature.media.state.MediaState
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager

class TabTrayControllerTest {

    private val store: TabTrayFragmentStore = mockk(relaxed = true)
    private val state: TabTrayFragmentState = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { store.state } returns state
    }

    @Test
    fun onCloseTab() {
        val tab = Tab(
            sessionId = "1",
            url = "",
            hostname = "",
            title = "",
            selected = true,
            mediaState = MediaState.None,
            icon = null
        )
        val session = Session("")
        val sessionManager: SessionManager = mockk(relaxed = true)
        val browsingModeManager: BrowsingModeManager = mockk()

        var tabClosed = false
        var verifyIsPrivate = false

        val controller = DefaultTabTrayController(
            mockk(relaxed = true),
            sessionManager,
            store,
            browsingModeManager,
            { sessions, isPrivate ->
                tabClosed = sessions.contains(session)
                verifyIsPrivate = isPrivate
            },
            { }
        )

        every { browsingModeManager.mode } returns BrowsingMode.Private
        every { sessionManager.findSessionById("1") } returns session
        controller.closeTab(tab)

        assertTrue(tabClosed)
        assertTrue(verifyIsPrivate)
    }

    @Test
    fun onCloseAllTabs() {
        val sessionList = listOf(
            Session(""),
            Session(""),
            Session("", private = true)
        )

        val sessionManager: SessionManager = mockk(relaxed = true)
        val browsingModeManager: BrowsingModeManager = mockk()

        var verifyOnlyPrivateTabs = false
        var verifyIsPrivate = false

        val controller = DefaultTabTrayController(
            mockk(relaxed = true),
            sessionManager,
            store,
            browsingModeManager,
            { sessions, isPrivate ->
                verifyOnlyPrivateTabs = sessions.all { it.private }
                verifyIsPrivate = isPrivate
            },
            { }
        )

        every { browsingModeManager.mode } returns BrowsingMode.Private
        every { sessionManager.sessions } returns sessionList
        controller.closeAllTabs()

        assertTrue(verifyOnlyPrivateTabs)
        assertTrue(verifyIsPrivate)
    }

    @Test
    fun onPauseMedia() {
        var pauseWasCalled = false
        val controller = DefaultTabTrayController(
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            { _, _ -> },
            { },
            pauseMediaUseCase = { pauseWasCalled = true }
        )

        controller.pauseMedia()
        assertTrue(pauseWasCalled)
    }

    @Test
    fun onPlayMedia() {
        var playWasCalled = false
        val controller = DefaultTabTrayController(
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            { _, _ -> },
            { },
            playMediaUseCase = { playWasCalled = true }
        )

        controller.playMedia()
        assertTrue(playWasCalled)
    }


}
