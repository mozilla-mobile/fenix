/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.recentlyclosed

import io.mockk.mockk
import io.mockk.verify
import mozilla.components.browser.state.state.ClosedTab
import org.junit.Before
import org.junit.Test
import org.mozilla.fenix.browser.browsingmode.BrowsingMode

class RecentlyClosedFragmentInteractorTest {

    lateinit var interactor: RecentlyClosedFragmentInteractor
    private val defaultRecentlyClosedController: DefaultRecentlyClosedController =
        mockk(relaxed = true)

    @Before
    fun setup() {
        interactor =
            RecentlyClosedFragmentInteractor(
                recentlyClosedController = defaultRecentlyClosedController
            )
    }

    @Test
    fun open() {
        val tab = ClosedTab(id = "tab-id", title = "Mozilla", url = "mozilla.org", createdAt = 1L)
        interactor.restore(tab)

        verify {
            defaultRecentlyClosedController.handleRestore(tab)
        }
    }

    @Test
    fun onCopyPressed() {
        val tab = ClosedTab(id = "tab-id", title = "Mozilla", url = "mozilla.org", createdAt = 1L)
        interactor.onCopyPressed(tab)

        verify {
            defaultRecentlyClosedController.handleCopyUrl(tab)
        }
    }

    @Test
    fun onSharePressed() {
        val tab = ClosedTab(id = "tab-id", title = "Mozilla", url = "mozilla.org", createdAt = 1L)
        interactor.onSharePressed(tab)

        verify {
            defaultRecentlyClosedController.handleShare(tab)
        }
    }

    @Test
    fun onOpenInNormalTab() {
        val tab = ClosedTab(id = "tab-id", title = "Mozilla", url = "mozilla.org", createdAt = 1L)
        interactor.onOpenInNormalTab(tab)

        verify {
            defaultRecentlyClosedController.handleOpen(tab, mode = BrowsingMode.Normal)
        }
    }

    @Test
    fun onOpenInPrivateTab() {
        val tab = ClosedTab(id = "tab-id", title = "Mozilla", url = "mozilla.org", createdAt = 1L)
        interactor.onOpenInPrivateTab(tab)

        verify {
            defaultRecentlyClosedController.handleOpen(tab, mode = BrowsingMode.Private)
        }
    }

    @Test
    fun onDeleteOne() {
        val tab = ClosedTab(id = "tab-id", title = "Mozilla", url = "mozilla.org", createdAt = 1L)
        interactor.onDeleteOne(tab)

        verify {
            defaultRecentlyClosedController.handleDeleteOne(tab)
        }
    }

    @Test
    fun onNavigateToHistory() {
        interactor.onNavigateToHistory()

        verify {
            defaultRecentlyClosedController.handleNavigateToHistory()
        }
    }
}
