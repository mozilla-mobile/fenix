/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.browser

import io.mockk.mockk
import io.mockk.verify
import mozilla.components.browser.state.state.ContentState
import mozilla.components.browser.state.state.TabSessionState
import org.junit.Test
import org.mozilla.fenix.tabstray.TabsTrayInteractor
import org.mozilla.fenix.tabstray.TrayPagerAdapter

class DefaultInactiveTabsInteractorTest {

    private val controller: InactiveTabsController = mockk(relaxed = true)
    private val interactor: TabsTrayInteractor = mockk(relaxed = true)

    @Test
    fun `WHEN the inactive tabs header is clicked THEN update the expansion state of the inactive tabs card`() {
        createInteractor().onInactiveTabsHeaderClicked(true)

        verify { controller.handleInactiveTabsHeaderClicked(true) }
    }

    @Test
    fun `WHEN the inactive tabs auto close dialog's close button is clicked THEN dismiss the dialog`() {
        createInteractor().onAutoCloseDialogCloseButtonClicked()

        verify { controller.handleInactiveTabsAutoCloseDialogDismiss() }
    }

    @Test
    fun `WHEN the enable inactive tabs auto close button is clicked THEN turn on the auto close feature`() {
        createInteractor().onEnableAutoCloseClicked()

        verify { controller.handleEnableInactiveTabsAutoCloseClicked() }
    }

    @Test
    fun `WHEN an inactive tab is clicked THEN open the tab`() {
        val tab = TabSessionState(
            id = "tabId",
            content = ContentState(
                url = "www.mozilla.com",
            ),
        )

        createInteractor().onInactiveTabClicked(tab)

        verify { controller.handleInactiveTabClicked(tab) }
        verify { interactor.onTabSelected(tab, TrayPagerAdapter.INACTIVE_TABS_FEATURE_NAME) }
    }

    @Test
    fun `WHEN an inactive tab is clicked to be closed THEN close the tab`() {
        val tab = TabSessionState(
            id = "tabId",
            content = ContentState(
                url = "www.mozilla.com",
            ),
        )

        createInteractor().onInactiveTabClosed(tab)

        verify { controller.handleCloseInactiveTabClicked(tab) }
        verify { interactor.onTabClosed(tab, TrayPagerAdapter.INACTIVE_TABS_FEATURE_NAME) }
    }

    @Test
    fun `WHEN the close all inactive tabs button is clicked THEN delete all inactive tabs`() {
        createInteractor().onDeleteAllInactiveTabsClicked()

        verify { controller.handleDeleteAllInactiveTabsClicked() }
    }

    private fun createInteractor(): DefaultInactiveTabsInteractor {
        return DefaultInactiveTabsInteractor(
            controller = controller,
            tabsTrayInteractor = interactor,
        )
    }
}
