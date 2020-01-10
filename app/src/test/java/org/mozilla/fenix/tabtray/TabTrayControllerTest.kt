/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabtray

import androidx.navigation.NavController
import androidx.navigation.NavDirections
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.feature.media.state.MediaState
import org.junit.Assert.assertFalse
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

    @Test
    fun onOpenTab() {
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
        val navController: NavController = mockk(relaxed = true)
        val expectedDirections = TabTrayFragmentDirections.actionTabTrayFragmentToBrowserFragment(null)

        val controller = DefaultTabTrayController(
            navController,
            sessionManager,
            store,
            mockk(relaxed = true),
            { _, _ -> },
            { }
        )

        every { sessionManager.findSessionById("1") } returns session

        controller.openTab(tab)
        verify {
            sessionManager.select(session)
            navController.navigate(expectedDirections)
        }
    }

    @Test
    fun onSelectTab() {
        val tab = Tab(
            sessionId = "1",
            url = "",
            hostname = "",
            title = "",
            selected = true,
            mediaState = MediaState.None,
            icon = null
        )
        val controller = DefaultTabTrayController(
            mockk(relaxed = true),
            mockk(relaxed = true),
            store,
            mockk(relaxed = true),
            { _, _ -> },
            { }
        )

        every { state.mode.isEditing } returns true

        controller.selectTab(tab)
        verify { store.dispatch(TabTrayFragmentAction.SelectTab(tab)) }
    }

    @Test
    fun onDeselectTab() {
        val tab = Tab(
            sessionId = "1",
            url = "",
            hostname = "",
            title = "",
            selected = true,
            mediaState = MediaState.None,
            icon = null
        )
        val controller = DefaultTabTrayController(
            mockk(relaxed = true),
            mockk(relaxed = true),
            store,
            mockk(relaxed = true),
            { _, _ -> },
            { }
        )

        every { state.mode.isEditing } returns true

        controller.deselectTab(tab)
        verify {
            store.dispatch(TabTrayFragmentAction.DeselectTab(tab))
        }
    }

    @Test
    fun onNewTab() {
        val navController: NavController = mockk(relaxed = true)
        val expectedDirections = TabTrayFragmentDirections.actionTabTrayFragmentToSearchFragment(null)
        val controller = DefaultTabTrayController(
            navController,
            mockk(relaxed = true),
            store,
            mockk(relaxed = true),
            { _, _ -> },
            { }
        )

        controller.newTab()

        verify {
            navController.navigate(expectedDirections)
        }
    }

    @Test
    fun onEnterPrivateBrowsingMode() {
        var hasChangedModes = false
        val controller = DefaultTabTrayController(
            mockk(relaxed = true),
            mockk(relaxed = true),
            store,
            mockk(relaxed = true),
            { _, _ -> },
            { hasChangedModes = true }
        )

        controller.enterPrivateBrowsingMode()
        assertTrue(hasChangedModes)
    }

    @Test
    fun onExitPrivateBrowsingMode() {
        var hasChangedModes = false
        val controller = DefaultTabTrayController(
            mockk(relaxed = true),
            mockk(relaxed = true),
            store,
            mockk(relaxed = true),
            { _, _ -> },
            { hasChangedModes = true }
        )

        controller.exitPrivateBrowsingMode()
        assertTrue(hasChangedModes)
    }

    @Test
    fun onShouldAllowSelect() {
        val controller = DefaultTabTrayController(
            mockk(relaxed = true),
            mockk(relaxed = true),
            store,
            mockk(relaxed = true),
            { _, _ -> },
            { }
        )

        every { state.mode.isEditing } returns true
        var shouldAllowSelect = controller.shouldAllowSelect()
        assertTrue(shouldAllowSelect)

        every { state.mode.isEditing } returns false
        shouldAllowSelect = controller.shouldAllowSelect()
        assertFalse(shouldAllowSelect)
    }

    @Test
    fun onNavigateToCollectionCreator() {
        val navController: NavController = mockk(relaxed = true)
        val tab = Tab(
            sessionId = "1",
            url = "",
            hostname = "",
            title = "",
            selected = true,
            mediaState = MediaState.None,
            icon = null
        )

        val controller = DefaultTabTrayController(
            navController,
            mockk(relaxed = true),
            store,
            mockk(relaxed = true),
            { _, _ -> },
            { }
        )

        every { state.mode } returns TabTrayFragmentState.Mode.Editing(setOf<Tab>(tab))

        controller.navigateToCollectionCreator()

        verify {
            navController.navigate(any<NavDirections>())
        }
    }
}
