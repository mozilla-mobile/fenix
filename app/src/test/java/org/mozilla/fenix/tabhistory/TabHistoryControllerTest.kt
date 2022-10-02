/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabhistory

import androidx.navigation.NavController
import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TabHistoryControllerTest {

    private lateinit var navController: NavController
    private lateinit var goToHistoryIndexUseCase: SessionUseCases.GoToHistoryIndexUseCase
    private lateinit var duplicateTabUseCase: TabsUseCases.DuplicateTabUseCase
    private lateinit var currentItem: TabHistoryItem
    private lateinit var store: BrowserStore

    @Before
    fun setUp() {
        store = BrowserStore()
        val tabsUseCases = TabsUseCases(store)
        tabsUseCases.addTab("mozilla.org")
        navController = mockk(relaxed = true)
        goToHistoryIndexUseCase = spyk(SessionUseCases(store).goToHistoryIndex)
        duplicateTabUseCase = spyk(tabsUseCases.duplicateTab)
        currentItem = TabHistoryItem(
            index = 0,
            title = "",
            url = "",
            isSelected = true,
        )
    }

    private fun createDefaultTabHistoryController(tabId: String?): DefaultTabHistoryController {
        return DefaultTabHistoryController(
            navController = navController,
            goToHistoryIndexUseCase = goToHistoryIndexUseCase,
            duplicateTabUseCase = duplicateTabUseCase,
            customTabId = tabId,
        )
    }

    @Test
    fun handleGoToHistoryIndexNormalBrowsing() {
        val controller = createDefaultTabHistoryController(null)

        controller.handleGoToHistoryItem(currentItem)
        verify { navController.navigateUp() }
        verify { goToHistoryIndexUseCase.invoke(currentItem.index) }
    }

    @Test
    fun handleGoToHistoryIndexCustomTab() {
        val customTabId = "customTabId"
        val customTabController = createDefaultTabHistoryController(customTabId)

        customTabController.handleGoToHistoryItem(currentItem)
        verify { navController.navigateUp() }
        verify { goToHistoryIndexUseCase.invoke(currentItem.index, customTabId) }
    }

    @Test
    fun handleGoToHistoryIndexNewTabNormalBrowsing() {
        val controller = createDefaultTabHistoryController(null)
        val onSuccess = mockk<() -> Unit>()
        every { onSuccess.invoke() } just Runs

        val res = controller.handleGoToHistoryItemNewTab(currentItem, onSuccess)
        assertTrue(res)
        verifyOrder {
            duplicateTabUseCase.invoke(true)
            onSuccess.invoke()
            navController.navigateUp()
            goToHistoryIndexUseCase.invoke(currentItem.index)
        }
    }

    @Test
    fun handleGoToHistoryIndexNewTabCustomTab() {
        val customTabId = "customTabId"
        val controller = createDefaultTabHistoryController(customTabId)
        val onSuccess = mockk<() -> Unit>()

        val res = controller.handleGoToHistoryItemNewTab(currentItem, onSuccess)
        assertFalse(res)
        verify {
            duplicateTabUseCase wasNot Called
            onSuccess wasNot Called
            navController wasNot Called
            goToHistoryIndexUseCase wasNot Called
        }
    }
}
