/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray

import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import mozilla.components.browser.state.state.ContentState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.storage.sync.Tab
import org.junit.Test
import org.mozilla.fenix.selection.SelectionHolder

class DefaultTabsTrayInteractorTest {

    private val controller: TabsTrayController = mockk(relaxed = true)
    private val trayInteractor = DefaultTabsTrayInteractor(
        controller = controller,
    )

    @Test
    fun `GIVEN user selecting a new tray page WHEN onTrayPositionSelected is called THEN the Interactor delegates the controller`() {
        trayInteractor.onTrayPositionSelected(14, true)

        verifySequence { controller.handleTrayScrollingToPosition(14, true) }
    }

    @Test
    fun `GIVEN user selecting a new browser tab WHEN onBrowserTabSelected is called THEN the Interactor delegates the controller`() {
        trayInteractor.onBrowserTabSelected()

        verifySequence { controller.handleNavigateToBrowser() }
    }

    @Test
    fun `GIVEN user confirmed downloads cancellation WHEN onDeletePrivateTabWarningAccepted is called THEN the Interactor delegates the controller`() {
        trayInteractor.onDeletePrivateTabWarningAccepted("testTabId")

        verifySequence { controller.handleDeleteTabWarningAccepted("testTabId") }
    }

    @Test
    fun `GIVEN user deleted multiple browser tabs WHEN onDeleteTabs is called THEN the Interactor delegates the controller`() {
        val tabsToDelete = listOf<TabSessionState>(mockk(), mockk())

        trayInteractor.onDeleteTabs(tabsToDelete)

        verifySequence { controller.handleMultipleTabsDeletion(tabsToDelete) }
    }

    @Test
    fun `GIVEN user has selected at least one tab WHEN the user forces the selected tabs to become inactive THEN the Interactor delegates to the controller`() {
        val tabs = listOf<TabSessionState>(mockk(), mockk())

        trayInteractor.onInactiveDebugClicked(tabs)

        verify { controller.forceTabsAsInactive(tabs) }
    }

    @Test
    fun `WHEN a tab is dragged to be repositioned THEN delegate to the controller`() {
        val tabId = "42"
        val targetId = "789"
        val placeAfter = true
        trayInteractor.onTabsMove(tabId, targetId, placeAfter)

        verify { controller.handleTabsMove(tabId, targetId, placeAfter) }
    }

    @Test
    fun `WHEN the tabs tray FAB is clicked THEN delegate to the controller`() {
        trayInteractor.onFabClicked(true)
        verify { controller.handleOpeningNewTab(true) }

        trayInteractor.onFabClicked(false)
        verify { controller.handleOpeningNewTab(false) }
    }

    @Test
    fun `WHEN the recently closed item is clicked THEN delegate to the controller`() {
        trayInteractor.onRecentlyClosedClicked()

        verify { controller.handleNavigateToRecentlyClosed() }
    }

    @Test
    fun `WHEN a synced tab is clicked THEN delegate to the controller`() {
        val tab = mockk<Tab>()
        trayInteractor.onSyncedTabClicked(tab)

        verify { controller.handleSyncedTabClick(tab) }
    }

    @Test
    fun `WHEN the media button on a tab is clicked THEN delegate to the controller`() {
        val tab = mockk<TabSessionState>()
        trayInteractor.onMediaClicked(tab)

        verify { controller.handleMediaClicked(tab) }
    }

    @Test
    fun `WHEN the inactive tabs header is clicked THEN update the expansion state of the inactive tabs card`() {
        trayInteractor.onHeaderClicked(true)

        verify { controller.updateCardExpansion(true) }
    }

    @Test
    fun `WHEN the inactive tabs auto close dialog's close button is clicked THEN dismiss the dialog`() {
        trayInteractor.onCloseClicked()

        verify { controller.dismissAutoCloseDialog() }
    }

    @Test
    fun `WHEN the enable inactive tabs auto close button is clicked THEN turn on the auto close feature`() {
        trayInteractor.onEnableAutoCloseClicked()

        verify { controller.enableInactiveTabsAutoClose() }
    }

    @Test
    fun `WHEN an inactive tab is clicked THEN open the tab`() {
        val tab = TabSessionState(
            id = "tabId",
            content = ContentState(
                url = "www.mozilla.com",
            ),
        )

        trayInteractor.onInactiveTabClicked(tab)

        verify { controller.openInactiveTab(tab) }
    }

    @Test
    fun `WHEN an inactive tab is clicked to be closed THEN close the tab`() {
        val tab = TabSessionState(
            id = "tabId",
            content = ContentState(
                url = "www.mozilla.com",
            ),
        )

        trayInteractor.onInactiveTabClosed(tab)

        verify { controller.closeInactiveTab(tab) }
    }

    @Test
    fun `WHEN the close all inactive tabs button is clicked THEN delete all inactive tabs`() {
        trayInteractor.onDeleteAllInactiveTabsClicked()

        verify { controller.deleteAllInactiveTabs() }
    }

    @Test
    fun `WHEN a tab is re-ordered by the user THEN delegate to the controller`() {
        trayInteractor.onTabsMove("123", "1234", true)

        verify { controller.handleTabsMove("123", "1234", true) }
    }

    @Test
    fun `WHEN a tab is selected during multi-selection THEN delegate to the controller`() {
        val tab: TabSessionState = mockk()
        val selectionHolder = mockk<SelectionHolder<TabSessionState>>()
        trayInteractor.onMultiSelectClicked(tab, selectionHolder, "source")

        verify { controller.handleMultiSelectTabClick(tab, selectionHolder, "source") }
    }

    @Test
    fun `WHEN a tab is long pressed THEN delegate to the controller`() {
        val tab: TabSessionState = mockk()
        val selectionHolder = mockk<SelectionHolder<TabSessionState>>()
        trayInteractor.onTabLongClicked(tab, selectionHolder)

        verify { controller.handleTabLongClick(tab, selectionHolder) }
    }
}
