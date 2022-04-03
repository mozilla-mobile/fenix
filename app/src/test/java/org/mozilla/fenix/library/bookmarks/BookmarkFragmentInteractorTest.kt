/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.bookmarks

import io.mockk.mockk
import io.mockk.verify
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarkNodeType
import mozilla.components.support.test.robolectric.testContext
import mozilla.telemetry.glean.testing.GleanTestRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.GleanMetrics.BookmarksManagement
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner

@SuppressWarnings("TooManyFunctions", "LargeClass")
@RunWith(FenixRobolectricTestRunner::class) // For GleanTestRule
class BookmarkFragmentInteractorTest {

    @get:Rule
    val gleanTestRule = GleanTestRule(testContext)

    private lateinit var interactor: BookmarkFragmentInteractor

    private val bookmarkController: DefaultBookmarkController = mockk(relaxed = true)
    private val metrics: MetricController = mockk(relaxed = true)

    private val item = BookmarkNode(BookmarkNodeType.ITEM, "456", "123", 0u, "Mozilla", "http://mozilla.org", 0, null)
    private val separator = BookmarkNode(BookmarkNodeType.SEPARATOR, "789", "123", 1u, null, null, 0, null)
    private val subfolder = BookmarkNode(BookmarkNodeType.FOLDER, "987", "123", 0u, "Subfolder", null, 0, listOf())
    private val tree: BookmarkNode = BookmarkNode(
        BookmarkNodeType.FOLDER, "123", null, 0u, "Mobile", null, 0, listOf(item, separator, item, subfolder)
    )

    @Before
    fun setup() {
        interactor =
            BookmarkFragmentInteractor(
                bookmarksController = bookmarkController,
                metrics = metrics
            )
    }

    @Test
    fun `update bookmarks tree`() {
        interactor.onBookmarksChanged(tree)

        verify {
            bookmarkController.handleBookmarkChanged(tree)
        }
    }

    @Test
    fun `open a bookmark item`() {
        interactor.open(item)

        verify { bookmarkController.handleBookmarkTapped(item) }
        assertTrue(BookmarksManagement.open.testHasValue())
        assertEquals(1, BookmarksManagement.open.testGetValue().size)
        assertNull(BookmarksManagement.open.testGetValue().single().extra)
    }

    @Test(expected = IllegalStateException::class)
    fun `open a separator`() {
        interactor.open(item.copy(type = BookmarkNodeType.SEPARATOR))
    }

    @Test
    fun `expand a level of bookmarks`() {
        interactor.open(tree)

        verify {
            bookmarkController.handleBookmarkExpand(tree)
        }
    }

    @Test
    fun `switch between bookmark selection modes`() {
        interactor.onSelectionModeSwitch(BookmarkFragmentState.Mode.Normal())

        verify {
            bookmarkController.handleSelectionModeSwitch()
        }
    }

    @Test
    fun `press the edit bookmark button`() {
        interactor.onEditPressed(item)

        verify {
            bookmarkController.handleBookmarkEdit(item)
        }
    }

    @Test
    fun `select a bookmark item`() {
        interactor.select(item)

        verify {
            bookmarkController.handleBookmarkSelected(item)
        }
    }

    @Test
    fun `deselect a bookmark item`() {
        interactor.deselect(item)

        verify {
            bookmarkController.handleBookmarkDeselected(item)
        }
    }

    @Test
    fun `deselectAll bookmark items`() {
        interactor.onAllBookmarksDeselected()

        verify {
            bookmarkController.handleAllBookmarksDeselected()
        }
    }

    @Test
    fun `copy a bookmark item`() {
        interactor.onCopyPressed(item)

        verify { bookmarkController.handleCopyUrl(item) }
        assertTrue(BookmarksManagement.copied.testHasValue())
        assertEquals(1, BookmarksManagement.copied.testGetValue().size)
        assertNull(BookmarksManagement.copied.testGetValue().single().extra)
    }

    @Test
    fun `share a bookmark item`() {
        interactor.onSharePressed(item)

        verify { bookmarkController.handleBookmarkSharing(item) }
        assertTrue(BookmarksManagement.shared.testHasValue())
        assertEquals(1, BookmarksManagement.shared.testGetValue().size)
        assertNull(BookmarksManagement.shared.testGetValue().single().extra)
    }

    @Test
    fun `open a bookmark item in a new tab`() {
        interactor.onOpenInNormalTab(item)

        verify { bookmarkController.handleOpeningBookmark(item, BrowsingMode.Normal) }
        assertTrue(BookmarksManagement.openInNewTab.testHasValue())
        assertEquals(1, BookmarksManagement.openInNewTab.testGetValue().size)
        assertNull(BookmarksManagement.openInNewTab.testGetValue().single().extra)
    }

    @Test
    fun `open a bookmark item in a private tab`() {
        interactor.onOpenInPrivateTab(item)

        verify { bookmarkController.handleOpeningBookmark(item, BrowsingMode.Private) }
        assertTrue(BookmarksManagement.openInPrivateTab.testHasValue())
        assertEquals(1, BookmarksManagement.openInPrivateTab.testGetValue().size)
        assertNull(BookmarksManagement.openInPrivateTab.testGetValue().single().extra)
    }

    @Test
    fun `delete a bookmark item`() {
        interactor.onDelete(setOf(item))

        verify {
            bookmarkController.handleBookmarkDeletion(setOf(item), BookmarkRemoveType.SINGLE)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `delete a separator`() {
        interactor.onDelete(setOf(item, item.copy(type = BookmarkNodeType.SEPARATOR)))
    }

    @Test
    fun `delete a bookmark folder`() {
        interactor.onDelete(setOf(subfolder))

        verify {
            bookmarkController.handleBookmarkFolderDeletion(setOf(subfolder))
        }
    }

    @Test
    fun `delete multiple bookmarks`() {
        interactor.onDelete(setOf(item, subfolder))

        verify {
            bookmarkController.handleBookmarkDeletion(setOf(item, subfolder), BookmarkRemoveType.MULTIPLE)
        }
    }

    @Test
    fun `press the back button`() {
        interactor.onBackPressed()

        verify {
            bookmarkController.handleBackPressed()
        }
    }

    @Test
    fun `request a sync`() {
        interactor.onRequestSync()

        verify {
            bookmarkController.handleRequestSync()
        }
    }

    @Test
    fun `WHEN onSearch is called THEN call controller handleSearch`() {
        interactor.onSearch()

        verify {
            bookmarkController.handleSearch()
        }
    }
}
