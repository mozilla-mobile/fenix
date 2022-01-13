/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.bookmarks

import android.content.ClipboardManager
import android.content.Context
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDirections
import io.mockk.Runs
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import mozilla.appservices.places.BookmarkRoot
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarkNodeType
import mozilla.components.concept.storage.BookmarkNodeType.FOLDER
import mozilla.components.concept.storage.BookmarksStorage
import mozilla.components.feature.tabs.TabsUseCases
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.Services
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.bookmarkStorage
import org.mozilla.fenix.ext.components

@Suppress("TooManyFunctions", "LargeClass")
class BookmarkControllerTest {

    private val bookmarkStore = spyk(BookmarkFragmentStore(BookmarkFragmentState(null)))
    private val context: Context = mockk(relaxed = true)
    private val scope = TestCoroutineScope()
    private val clipboardManager: ClipboardManager = mockk(relaxUnitFun = true)
    private val navController: NavController = mockk(relaxed = true)
    private val sharedViewModel: BookmarksSharedViewModel = mockk()
    private val tabsUseCases: TabsUseCases = mockk()
    private val homeActivity: HomeActivity = mockk(relaxed = true)
    private val services: Services = mockk(relaxed = true)
    private val addNewTabUseCase: TabsUseCases.AddNewTabUseCase = mockk(relaxed = true)
    private val bookmarkStorage: BookmarksStorage = mockk()

    private val item =
        BookmarkNode(BookmarkNodeType.ITEM, "456", "123", 0, "Mozilla", "http://mozilla.org", 0, null)
    private val subfolder =
        BookmarkNode(BookmarkNodeType.FOLDER, "987", "123", 0, "Subfolder", null, 0, listOf())
    private val childItem = BookmarkNode(
        BookmarkNodeType.ITEM,
        "987",
        "123",
        2,
        "Firefox",
        "https://www.mozilla.org/en-US/firefox/",
        0,
        null
    )
    private val tree = BookmarkNode(
        BookmarkNodeType.FOLDER,
        "123",
        null,
        0,
        "Mobile",
        null,
        0,
        listOf(item, item, childItem, subfolder)
    )
    private val root = BookmarkNode(
        BookmarkNodeType.FOLDER, BookmarkRoot.Root.id, null, 0, BookmarkRoot.Root.name, null, 0, null
    )

    @Before
    fun setup() {
        every { homeActivity.components.services } returns services
        every { navController.currentDestination } returns NavDestination("").apply {
            id = R.id.bookmarkFragment
        }
        every { bookmarkStore.dispatch(any()) } returns mockk()
        every { sharedViewModel.selectedFolder = any() } just runs
        every { tabsUseCases.addTab } returns addNewTabUseCase
    }

    @After
    fun cleanUp() {
        scope.cleanupTestCoroutines()
    }

    @Test
    fun `handleBookmarkChanged updates the selected bookmark node`() {
        createController().handleBookmarkChanged(tree)

        verify {
            sharedViewModel.selectedFolder = tree
            bookmarkStore.dispatch(BookmarkFragmentAction.Change(tree))
        }
    }

    @Test
    fun `handleBookmarkTapped should load the bookmark in a new tab`() {
        var invokePendingDeletionInvoked = false
        val flags = EngineSession.LoadUrlFlags.select(EngineSession.LoadUrlFlags.ALLOW_JAVASCRIPT_URL)

        createController(
            invokePendingDeletion = {
                invokePendingDeletionInvoked = true
            }
        ).handleBookmarkTapped(item)

        assertTrue(invokePendingDeletionInvoked)
        verify {
            homeActivity.openToBrowserAndLoad(
                item.url!!,
                true,
                BrowserDirection.FromBookmarks,
                flags = flags
            )
        }
    }

    @Test
    fun `handleBookmarkTapped should respect browsing mode`() {
        // if in normal mode, should be in normal mode
        every { homeActivity.browsingModeManager.mode } returns BrowsingMode.Normal

        val controller = createController()
        controller.handleBookmarkTapped(item)
        assertEquals(BrowsingMode.Normal, homeActivity.browsingModeManager.mode)

        // if in private mode, should be in private mode
        every { homeActivity.browsingModeManager.mode } returns BrowsingMode.Private

        controller.handleBookmarkTapped(item)
        assertEquals(BrowsingMode.Private, homeActivity.browsingModeManager.mode)
    }

    @Test
    fun `handleBookmarkExpand clears selection and invokes pending deletions`() {
        var invokePendingDeletionInvoked = false
        createController(
            invokePendingDeletion = {
                invokePendingDeletionInvoked = true
            }
        ).handleBookmarkExpand(tree)

        assertTrue(invokePendingDeletionInvoked)
    }

    @Test
    fun `handleBookmarkExpand should refresh and change the active bookmark node`() {
        var loadBookmarkNodeInvoked = false
        createController(
            loadBookmarkNode = {
                loadBookmarkNodeInvoked = true
                tree
            }
        ).handleBookmarkExpand(tree)

        assertTrue(loadBookmarkNodeInvoked)
        coVerify {
            sharedViewModel.selectedFolder = tree
            bookmarkStore.dispatch(BookmarkFragmentAction.Change(tree))
        }
    }

    @Test
    fun `handleSelectionModeSwitch should invalidateOptionsMenu`() {
        createController().handleSelectionModeSwitch()

        verify {
            homeActivity.invalidateOptionsMenu()
        }
    }

    @Test
    fun `handleBookmarkEdit should navigate to the 'Edit' fragment`() {
        var invokePendingDeletionInvoked = false
        createController(
            invokePendingDeletion = {
                invokePendingDeletionInvoked = true
            }
        ).handleBookmarkEdit(item)

        assertTrue(invokePendingDeletionInvoked)
        verify {
            navController.navigate(
                BookmarkFragmentDirections.actionBookmarkFragmentToBookmarkEditFragment(
                    item.guid
                ),
                null
            )
        }
    }

    @Test
    fun `handleBookmarkSelected dispatches Select action when selecting a non-root folder`() {
        createController().handleBookmarkSelected(item)

        verify {
            bookmarkStore.dispatch(BookmarkFragmentAction.Select(item))
        }
    }

    @Test
    fun `handleBookmarkSelected should show a toast when selecting a root folder`() {
        val errorMessage = context.getString(R.string.bookmark_cannot_edit_root)

        var showSnackbarInvoked = false
        createController(
            showSnackbar = {
                assertEquals(errorMessage, it)
                showSnackbarInvoked = true
            }
        ).handleBookmarkSelected(root)

        assertTrue(showSnackbarInvoked)
    }

    @Test
    fun `handleBookmarkSelected does not select in Syncing mode`() {
        every { bookmarkStore.state.mode } returns BookmarkFragmentState.Mode.Syncing

        createController().handleBookmarkSelected(item)

        verify { bookmarkStore.dispatch(BookmarkFragmentAction.Select(item)) wasNot called }
    }

    @Test
    fun `handleBookmarkDeselected dispatches Deselect action`() {
        createController().handleBookmarkDeselected(item)

        verify {
            bookmarkStore.dispatch(BookmarkFragmentAction.Deselect(item))
        }
    }

    @Test
    fun `handleCopyUrl should copy bookmark url to clipboard and show a toast`() {
        val urlCopiedMessage = context.getString(R.string.url_copied)

        var showSnackbarInvoked = false
        createController(
            showSnackbar = {
                assertEquals(urlCopiedMessage, it)
                showSnackbarInvoked = true
            }
        ).handleCopyUrl(item)

        assertTrue(showSnackbarInvoked)
    }

    @Test
    fun `handleBookmarkSharing should navigate to the 'Share' fragment`() {
        val navDirectionsSlot = slot<NavDirections>()
        every { navController.navigate(capture(navDirectionsSlot), null) } just Runs

        createController().handleBookmarkSharing(item)

        verify {
            navController.navigate(navDirectionsSlot.captured, null)
        }
    }

    @Test
    fun `handleBookmarkTapped should open the bookmark`() {
        var invokePendingDeletionInvoked = false
        val flags =
            EngineSession.LoadUrlFlags.select(EngineSession.LoadUrlFlags.ALLOW_JAVASCRIPT_URL)

        createController(
            invokePendingDeletion = {
                invokePendingDeletionInvoked = true
            }
        ).handleBookmarkTapped(item)

        assertTrue(invokePendingDeletionInvoked)
        verify {
            homeActivity.openToBrowserAndLoad(
                item.url!!,
                true,
                BrowserDirection.FromBookmarks,
                flags = flags
            )
        }
    }

    @Test
    fun `handleOpeningBookmark should open the bookmark a new 'Normal' tab`() {
        var invokePendingDeletionInvoked = false
        var showTabTrayInvoked = false
        createController(
            invokePendingDeletion = {
                invokePendingDeletionInvoked = true
            },
            showTabTray = {
                showTabTrayInvoked = true
            }
        ).handleOpeningBookmark(item, BrowsingMode.Normal)

        assertTrue(invokePendingDeletionInvoked)
        assertTrue(showTabTrayInvoked)
        verifyOrder {
            homeActivity.browsingModeManager.mode = BrowsingMode.Normal
            addNewTabUseCase.invoke(item.url!!, private = false)
        }
    }

    @Test
    fun `handleOpeningBookmark should open the bookmark a new 'Private' tab`() {
        var invokePendingDeletionInvoked = false
        var showTabTrayInvoked = false
        createController(
            invokePendingDeletion = {
                invokePendingDeletionInvoked = true
            },
            showTabTray = {
                showTabTrayInvoked = true
            }
        ).handleOpeningBookmark(item, BrowsingMode.Private)

        assertTrue(invokePendingDeletionInvoked)
        assertTrue(showTabTrayInvoked)
        verifyOrder {
            homeActivity.browsingModeManager.mode = BrowsingMode.Private
            addNewTabUseCase.invoke(item.url!!, private = true)
        }
    }

    @Test
    fun `handleBookmarkDeletion for an item should properly call a passed in delegate`() {
        var deleteBookmarkNodesInvoked = false
        createController(
            deleteBookmarkNodes = { nodes, event ->
                assertEquals(setOf(item), nodes)
                assertEquals(Event.RemoveBookmark, event)
                deleteBookmarkNodesInvoked = true
            }
        ).handleBookmarkDeletion(setOf(item), Event.RemoveBookmark)

        assertTrue(deleteBookmarkNodesInvoked)
    }

    @Test
    fun `handleBookmarkDeletion for multiple bookmarks should properly call a passed in delegate`() {
        var deleteBookmarkNodesInvoked = false
        createController(
            deleteBookmarkNodes = { nodes, event ->
                assertEquals(setOf(item, subfolder), nodes)
                assertEquals(Event.RemoveBookmarks, event)
                deleteBookmarkNodesInvoked = true
            }
        ).handleBookmarkDeletion(setOf(item, subfolder), Event.RemoveBookmarks)

        assertTrue(deleteBookmarkNodesInvoked)
    }

    @Test
    fun `handleBookmarkDeletion for a folder should properly call the delete folder delegate`() {
        var deleteBookmarkFolderInvoked = false
        createController(
            deleteBookmarkFolder = { nodes ->
                assertEquals(setOf(subfolder), nodes)
                deleteBookmarkFolderInvoked = true
            }
        ).handleBookmarkFolderDeletion(setOf(subfolder))

        assertTrue(deleteBookmarkFolderInvoked)
    }

    @Test
    fun `handleRequestSync dispatches actions in the correct order`() {
        every { homeActivity.components.backgroundServices.accountManager } returns mockk(relaxed = true)
        coEvery { homeActivity.bookmarkStorage.getBookmark(any()) } returns tree

        createController().handleRequestSync()

        verifyOrder {
            bookmarkStore.dispatch(BookmarkFragmentAction.StartSync)
            bookmarkStore.dispatch(BookmarkFragmentAction.FinishSync)
        }
    }

    @Test
    fun `handleBackPressed with one item in backstack should trigger handleBackPressed in NavController`() {
        every { bookmarkStore.state.guidBackstack } returns listOf(tree.guid)
        every { bookmarkStore.state.tree } returns tree

        var invokePendingDeletionInvoked = false
        createController(
            invokePendingDeletion = {
                invokePendingDeletionInvoked = true
            }
        ).handleBackPressed()

        assertTrue(invokePendingDeletionInvoked)

        verify {
            navController.popBackStack()
        }
    }

    @Test
    fun `GIVEN user bookmarks exist WHEN startSearch is called THEN dispatch SearchStarted containing all user bookmarks`() {
        val bookmark1: BookmarkNode = mockk(relaxed = true)
        val folder1: BookmarkNode = mockk(relaxed = true) {
            every { type } returns FOLDER
            every { children } returns listOf(bookmark1)
        }
        val bookmark2: BookmarkNode = mockk(relaxed = true)
        val rootNode = spyk(root) {
            every { children } returns listOf(folder1, bookmark2)
        }
        coEvery { bookmarkStorage.getTree(guid = BookmarkRoot.Root.id, recursive = true) } returns rootNode

        scope.launch { createController().startSearch() }

        coVerify { bookmarkStore.dispatch(BookmarkFragmentAction.SearchStarted(listOf(folder1, bookmark1, bookmark2))) }
    }

    @Test
    fun `GIVEN not already in search mode and a first valid query is entered WHEN isFirstValidQuery is called THEN return true`() {
        val result = createController().isFirstValidQuery("", "f")

        assertTrue(result)
    }

    @Test
    fun `GIVEN already in search mode and a new valid query is entered after a previous empty one WHEN isFirstValidQuery is called THEN return false`() {
        val store = BookmarkFragmentStore(
            BookmarkFragmentState(
                tree = null,
                mode = BookmarkFragmentState.Mode.Searching(emptyList())
            )
        )
        val controller = createController(store = store)

        val result = controller.isFirstValidQuery("", "f")

        assertFalse(result)
    }

    @Test
    fun `WHEN handleSearchEnded is called THEN dispatch SearchEnded`() {
        createController().handleSearchEnded()

        coVerify { bookmarkStore.dispatch(BookmarkFragmentAction.SearchEnded) }
    }

    @Test
    fun `GIVEN searchable nodes exist in BookmarkFragmentState#Mode#Searching WHEN handleQuery is called THEN dispatch UpdateQueriedItems with all nodes containing the query in title or url`() {
        val searchableNodes = tree.children!!
        val store = spyk(
            BookmarkFragmentStore(
                BookmarkFragmentState(
                    tree = root,
                    mode = BookmarkFragmentState.Mode.Searching(searchableNodes)
                )
            )
        )
        val controller = createController(store = store)

        controller.handleQuery("fire", "firef")
        controller.handleQuery("", "FireFox")
        controller.handleQuery("", "moziLLA")

        coVerify(exactly = 0) { store.dispatch(BookmarkFragmentAction.SearchStarted(searchableNodes)) }
        coVerify(exactly = 0) { store.dispatch(BookmarkFragmentAction.UpdateQueriedItems(searchableNodes)) }
    }

    @Suppress("LongParameterList")
    private fun createController(
        store: BookmarkFragmentStore = bookmarkStore,
        loadBookmarkNode: suspend (String) -> BookmarkNode? = { _ -> null },
        showSnackbar: (String) -> Unit = { _ -> },
        deleteBookmarkNodes: (Set<BookmarkNode>, Event) -> Unit = { _, _ -> },
        deleteBookmarkFolder: (Set<BookmarkNode>) -> Unit = { _ -> },
        invokePendingDeletion: () -> Unit = { },
        showTabTray: () -> Unit = { }
    ): DefaultBookmarkController {
        return DefaultBookmarkController(
            activity = homeActivity,
            navController = navController,
            clipboardManager = clipboardManager,
            scope = scope,
            store = store,
            sharedViewModel = sharedViewModel,
            tabsUseCases = tabsUseCases,
            loadBookmarkNode = loadBookmarkNode,
            bookmarksStorage = bookmarkStorage,
            showSnackbar = showSnackbar,
            deleteBookmarkNodes = deleteBookmarkNodes,
            deleteBookmarkFolder = deleteBookmarkFolder,
            invokePendingDeletion = invokePendingDeletion,
            showTabTray = showTabTray
        )
    }
}
