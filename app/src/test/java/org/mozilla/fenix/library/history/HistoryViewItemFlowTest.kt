/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import android.content.res.Resources
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.support.test.robolectric.testContext
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.components.history.HistoryDB
import org.mozilla.fenix.components.history.PagedHistoryProvider
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
class HistoryViewItemFlowTest {

    private val historyProvider: PagedHistoryProvider = mockk()
    private val browserStore: BrowserStore = mockk()
    private val accountManager: FxaAccountManager = mockk()

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())

        every { accountManager.authenticatedAccount() } returns mockk()
        every { browserStore.state } returns BrowserState()

        // Loading first page
        coEvery {
            historyProvider.getHistory(0, 75, null)
        } returns listOf(
            createRegularHistory(isRemote = false, visitedAt = 1658496038199),
            createRegularHistory(isRemote = false, visitedAt = 1658496038299),
            createRegularHistory(isRemote = true, visitedAt = 1658496038399),
            createRegularHistory(isRemote = true, visitedAt = 1658496038499),
            createRegularHistory(isRemote = true, visitedAt = 1658496038599),
            createRegularHistory(isRemote = true, visitedAt = 1658496038699),
        )

        // Loading second page
        coEvery {
            historyProvider.getHistory(75, 25, null)
        } returns listOf()
    }

    @Test
    fun `WHEN empty flow changes THEN empty item is either added or removed`() = runTest {
        val flow = createHistoryViewItemFlow()
        var adapter: HistoryAdapter = mockk()

        val job = launch {
            flow.historyFlow.collectLatest {
                adapter = HistoryAdapter(mockk()) {}
                adapter.submitData(it)
            }
        }

        flow.setEmptyState(true)
        advanceUntilIdle()
        var viewItems = adapter.snapshot().items
        assertNotNull(viewItems.find { it is HistoryViewItem.EmptyHistoryItem })

        flow.setEmptyState(false)
        advanceUntilIdle()
        viewItems = adapter.snapshot().items
        assertNull(viewItems.find { it is HistoryViewItem.EmptyHistoryItem })

        job.cancel()
    }

    @Test
    fun `WHEN delete flow is updated with items THEN matching items are removed`() = runTest {
        var adapter: HistoryAdapter = mockk()
        val flow = createHistoryViewItemFlow()
        val job = launch {
            flow.historyFlow.collectLatest {
                adapter = HistoryAdapter(mockk()) {}
                adapter.submitData(it)
            }
        }

        advanceUntilIdle()
        var viewItems = adapter.snapshot().items

        // Deleting first history item in the list.
        val itemToDelete = viewItems.find { it is HistoryViewItem.HistoryItem }?.let {
            (it as HistoryViewItem.HistoryItem)
        } ?: throw RuntimeException()

        val pendingDeletion = itemToDelete.data.toPendingDeletionHistory()
        flow.setDeleteItems(setOf(pendingDeletion), setOf())

        advanceUntilIdle()
        viewItems = adapter.snapshot().items

        assertNull(viewItems.find { it == itemToDelete })
        job.cancel()
    }

    @Test
    fun `WHEN data source is mixed GIVEN data set contains a header THEN every header has spacing above it`() {
        testHeaderStopSpacing(null)
    }

    @Test
    fun `WHEN data source is local GIVEN data set contains a header THEN every header has spacing above it`() {
        testHeaderStopSpacing(false)
    }

    @Test
    fun `WHEN data source is remote GIVEN data set contains a header THEN the first header does not have spacing above it`() {
        testHeaderStopSpacing(true)
    }

    private fun testHeaderStopSpacing(isRemote: Boolean?) = runTest {
        coEvery {
            historyProvider.getHistory(0, 25, null)
        } returns listOf(
            createRegularHistory(
                isRemote = false,
                visitedAt = Calendar.getInstance().timeInMillis
            ),
            createRegularHistory(
                isRemote = false,
                visitedAt = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -1)
                }.timeInMillis
            ),
            createRegularHistory(
                isRemote = false,
                visitedAt = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -4)
                }.timeInMillis
            ),
            createRegularHistory(
                isRemote = false,
                visitedAt = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -15)
                }.timeInMillis
            ),
            createRegularHistory(
                isRemote = false,
                visitedAt = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -35)
                }.timeInMillis
            ),
        )

        var adapter = HistoryAdapter(mockk()) {}
        val flow = createHistoryViewItemFlow(isRemote = isRemote)
        val job = launch {
            flow.historyFlow.collectLatest {
                adapter = HistoryAdapter(mockk()) {}
                adapter.submitData(it)
            }
        }

        advanceUntilIdle()
        val viewItems = adapter.snapshot().items

        if (isRemote == true) {
            var previousItem: HistoryViewItem? = null
            for (item in viewItems) {
                if (item is HistoryViewItem.TimeGroupHeader) {
                    assert(previousItem !is HistoryViewItem.TimeGroupSeparatorHistoryItem)
                    break
                }
                previousItem = item
            }
        } else {
            assert(
                viewItems.count { it is HistoryViewItem.TimeGroupHeader } ==
                    viewItems.count { it is HistoryViewItem.TimeGroupSeparatorHistoryItem }
            )
        }

        job.cancel()
    }

    private fun createRegularHistory(
        isRemote: Boolean = false,
        visitedAt: Long = 1658496038799,
    ) = HistoryDB.Regular(
        title = "Internet for people, not profit",
        url = "www.mozilla.org",
        visitedAt = visitedAt,
        selected = false,
        isRemote = isRemote
    )

    private fun createHistoryViewItemFlow(
        historyProvider: PagedHistoryProvider = this@HistoryViewItemFlowTest.historyProvider,
        browserStore: BrowserStore = this@HistoryViewItemFlowTest.browserStore,
        isRemote: Boolean? = null,
        resources: Resources = testContext.resources,
        accountManager: FxaAccountManager = this@HistoryViewItemFlowTest.accountManager,
        scope: CoroutineScope = TestScope(),
    ) = HistoryViewItemFlow(
        historyProvider = historyProvider,
        browserStore = browserStore,
        isRemote = isRemote,
        resources = resources,
        accountManager = accountManager,
        scope = scope,
    )
}
