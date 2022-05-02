/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray

import com.google.android.material.tabs.TabLayout
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mozilla.components.service.glean.testing.GleanTestRule
import mozilla.components.support.test.libstate.ext.waitUntilIdle
import mozilla.components.support.test.middleware.CaptureActionsMiddleware
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.GleanMetrics.TabsTray
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner

@RunWith(FenixRobolectricTestRunner::class) // for gleanTestRule
class TabLayoutObserverTest {
    private val interactor = mockk<TabsTrayInteractor>(relaxed = true)
    private lateinit var store: TabsTrayStore
    private val middleware = CaptureActionsMiddleware<TabsTrayState, TabsTrayAction>()

    @get:Rule
    val gleanTestRule = GleanTestRule(testContext)

    @Before
    fun setup() {
        store = TabsTrayStore(middlewares = listOf(middleware))
    }

    @Test
    fun `WHEN tab is selected THEN notify the interactor`() {
        val observer = TabLayoutObserver(interactor)
        val tab = mockk<TabLayout.Tab>()
        every { tab.position } returns 1
        assertFalse(TabsTray.privateModeTapped.testHasValue())

        observer.onTabSelected(tab)

        store.waitUntilIdle()

        verify { interactor.onTrayPositionSelected(1, false) }
        assertTrue(TabsTray.privateModeTapped.testHasValue())

        every { tab.position } returns 0
        assertFalse(TabsTray.normalModeTapped.testHasValue())

        observer.onTabSelected(tab)

        store.waitUntilIdle()

        verify { interactor.onTrayPositionSelected(0, true) }
        assertTrue(TabsTray.normalModeTapped.testHasValue())

        every { tab.position } returns 2
        assertFalse(TabsTray.syncedModeTapped.testHasValue())

        observer.onTabSelected(tab)

        store.waitUntilIdle()

        verify { interactor.onTrayPositionSelected(2, true) }
        assertTrue(TabsTray.syncedModeTapped.testHasValue())
    }

    @Test
    fun `WHEN observer is first started THEN do not smooth scroll`() {
        val observer = TabLayoutObserver(interactor)
        val tab = mockk<TabLayout.Tab>()
        every { tab.position } returns 1
        assertFalse(TabsTray.privateModeTapped.testHasValue())

        observer.onTabSelected(tab)

        verify { interactor.onTrayPositionSelected(1, false) }
        assertTrue(TabsTray.privateModeTapped.testHasValue())

        observer.onTabSelected(tab)

        verify { interactor.onTrayPositionSelected(1, true) }
        assertTrue(TabsTray.privateModeTapped.testHasValue())
    }
}
