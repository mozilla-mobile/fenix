/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.res.Resources
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import mozilla.components.concept.engine.prompt.ShareData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner

// Robolectric needed for `onShareItem()`
@ExperimentalCoroutinesApi
@RunWith(FenixRobolectricTestRunner::class)
class HistoryControllerTest {
    private val historyItem = HistoryItem(0, "title", "url", 0.toLong())
    private val scope: CoroutineScope = TestCoroutineScope()
    private val store: HistoryFragmentStore = mockk(relaxed = true)
    private val state: HistoryFragmentState = mockk(relaxed = true)
    private val navController: NavController = mockk(relaxed = true)
    private val resources: Resources = mockk(relaxed = true)
    private val snackbar: FenixSnackbar = mockk(relaxed = true)
    private val clipboardManager: ClipboardManager = mockk(relaxed = true)
    private val openInBrowser: (HistoryItem, BrowsingMode?) -> Unit = mockk(relaxed = true)
    private val displayDeleteAll: () -> Unit = mockk(relaxed = true)
    private val invalidateOptionsMenu: () -> Unit = mockk(relaxed = true)
    private val deleteHistoryItems: (Set<HistoryItem>) -> Unit = mockk(relaxed = true)
    private val syncHistory: suspend () -> Unit = mockk(relaxed = true)
    private val controller = DefaultHistoryController(
        store,
        navController,
        resources,
        snackbar,
        clipboardManager,
        scope,
        openInBrowser,
        displayDeleteAll,
        invalidateOptionsMenu,
        deleteHistoryItems,
        syncHistory
    )

    @Before
    fun setUp() {
        every { store.state } returns state
    }

    @Test
    fun onPressHistoryItemInNormalMode() {
        controller.handleOpen(historyItem)

        verify {
            openInBrowser(historyItem, null)
        }
    }

    @Test
    fun onOpenItemInNormalMode() {
        controller.handleOpen(historyItem, BrowsingMode.Normal)

        verify {
            openInBrowser(historyItem, BrowsingMode.Normal)
        }
    }

    @Test
    fun onOpenItemInPrivateMode() {
        controller.handleOpen(historyItem, BrowsingMode.Private)

        verify {
            openInBrowser(historyItem, BrowsingMode.Private)
        }
    }

    @Test
    fun onPressHistoryItemInEditMode() {
        every { state.mode } returns HistoryFragmentState.Mode.Editing(setOf())

        controller.handleSelect(historyItem)

        verify {
            store.dispatch(HistoryFragmentAction.AddItemForRemoval(historyItem))
        }
    }

    @Test
    fun onPressSelectedHistoryItemInEditMode() {
        every { state.mode } returns HistoryFragmentState.Mode.Editing(setOf(historyItem))

        controller.handleDeselect(historyItem)

        verify {
            store.dispatch(HistoryFragmentAction.RemoveItemForRemoval(historyItem))
        }
    }

    @Test
    fun onSelectHistoryItemDuringSync() {
        every { state.mode } returns HistoryFragmentState.Mode.Syncing

        controller.handleSelect(historyItem)

        verify(exactly = 0) {
            store.dispatch(HistoryFragmentAction.AddItemForRemoval(historyItem))
        }
    }

    @Test
    fun onBackPressedInNormalMode() {
        every { state.mode } returns HistoryFragmentState.Mode.Normal

        assertFalse(controller.handleBackPressed())
    }

    @Test
    fun onBackPressedInEditMode() {
        every { state.mode } returns HistoryFragmentState.Mode.Editing(setOf())

        assertTrue(controller.handleBackPressed())
        verify {
            store.dispatch(HistoryFragmentAction.ExitEditMode)
        }
    }

    @Test
    fun onModeSwitched() {
        controller.handleModeSwitched()

        verify {
            invalidateOptionsMenu.invoke()
        }
    }

    @Test
    fun onDeleteAll() {
        controller.handleDeleteAll()

        verify {
            displayDeleteAll.invoke()
        }
    }

    @Test
    fun onDeleteSome() {
        val itemsToDelete = setOf(historyItem)

        controller.handleDeleteSome(itemsToDelete)

        verify {
            deleteHistoryItems(itemsToDelete)
        }
    }

    @Test
    fun onCopyItem() {
        val clipdata = slot<ClipData>()

        controller.handleCopyUrl(historyItem)

        verify {
            clipboardManager.primaryClip = capture(clipdata)
            snackbar.show()
        }
        assertEquals(1, clipdata.captured.itemCount)
        assertEquals(historyItem.url, clipdata.captured.description.label)
        assertEquals(historyItem.url, clipdata.captured.getItemAt(0).text)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun onShareItem() {
        val directions = slot<NavDirections>()

        controller.handleShare(historyItem)

        // `verify` checks for referential equality.
        // This would fail as the NavDirections are created and used in place in the tested method.
        // Capture the NavDirections and `assert` for structural equality after.
        verify {
            navController.navigate(
                capture(directions)
            )
        }

        assertEquals(
            directions.captured.actionId,
            R.id.action_global_shareFragment
        )
        assertEquals(1, (directions.captured.arguments["data"] as Array<ShareData>).size)
        assertEquals(historyItem.title, (directions.captured.arguments["data"] as Array<ShareData>)[0].title)
        assertEquals(historyItem.url, (directions.captured.arguments["data"] as Array<ShareData>)[0].url)
    }

    @Test
    fun onRequestSync() {
        controller.handleRequestSync()

        verify(exactly = 2) {
            store.dispatch(any())
        }

        coVerifyOrder {
            store.dispatch(HistoryFragmentAction.StartSync)
            syncHistory.invoke()
            store.dispatch(HistoryFragmentAction.FinishSync)
        }
    }
}
