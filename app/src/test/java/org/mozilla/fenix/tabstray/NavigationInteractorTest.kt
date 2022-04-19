/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray

import android.content.Context
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.selector.getNormalOrPrivateTabs
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.content.DownloadState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.storage.sync.TabEntry
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.service.glean.testing.GleanTestRule
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.rule.MainCoroutineRule
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.GleanMetrics.Events
import org.mozilla.fenix.GleanMetrics.TabsTray
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.collections.CollectionsDialog
import org.mozilla.fenix.collections.show
import org.mozilla.fenix.components.TabCollectionStorage
import org.mozilla.fenix.components.bookmarks.BookmarksUseCase
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import mozilla.components.browser.state.state.createTab as createStateTab
import mozilla.components.browser.storage.sync.Tab as SyncTab

@RunWith(FenixRobolectricTestRunner::class) // for gleanTestRule
class NavigationInteractorTest {
    private lateinit var store: BrowserStore
    private lateinit var tabsTrayStore: TabsTrayStore
    private val testTab: TabSessionState = createStateTab(url = "https://mozilla.org")
    private val navController: NavController = mockk(relaxed = true)
    private val metrics: MetricController = mockk(relaxed = true)
    private val bookmarksUseCase: BookmarksUseCase = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val collectionStorage: TabCollectionStorage = mockk(relaxed = true)
    private val accountManager: FxaAccountManager = mockk(relaxed = true)
    private val activity: HomeActivity = mockk(relaxed = true)

    val coroutinesTestRule: MainCoroutineRule = MainCoroutineRule()
    val gleanTestRule = GleanTestRule(testContext)

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(coroutinesTestRule).around(gleanTestRule)

    private val testDispatcher = coroutinesTestRule.testDispatcher

    @Before
    fun setup() {
        store = BrowserStore(initialState = BrowserState(tabs = listOf(testTab)))
        tabsTrayStore = TabsTrayStore()
    }

    @Test
    fun `onTabTrayDismissed calls dismissTabTray on DefaultNavigationInteractor`() {
        var dismissTabTrayInvoked = false

        assertFalse(TabsTray.closed.testHasValue())

        createInteractor(
            dismissTabTray = {
                dismissTabTrayInvoked = true
            }
        ).onTabTrayDismissed()

        assertTrue(dismissTabTrayInvoked)
        assertTrue(TabsTray.closed.testHasValue())
    }

    @Test
    fun `onAccountSettingsClicked calls navigation on DefaultNavigationInteractor`() {
        every { accountManager.authenticatedAccount() }.answers { mockk(relaxed = true) }

        createInteractor().onAccountSettingsClicked()

        verify(exactly = 1) { navController.navigate(TabsTrayFragmentDirections.actionGlobalAccountSettingsFragment()) }
    }

    @Test
    fun `onAccountSettingsClicked when not logged in calls navigation to turn on sync`() {
        every { accountManager.authenticatedAccount() }.answers { null }

        createInteractor().onAccountSettingsClicked()

        verify(exactly = 1) { navController.navigate(TabsTrayFragmentDirections.actionGlobalTurnOnSync()) }
    }

    @Test
    fun `onTabSettingsClicked calls navigation on DefaultNavigationInteractor`() {
        createInteractor().onTabSettingsClicked()
        verify(exactly = 1) { navController.navigate(TabsTrayFragmentDirections.actionGlobalTabSettingsFragment()) }
    }

    @Test
    fun `onOpenRecentlyClosedClicked calls navigation on DefaultNavigationInteractor`() {
        assertFalse(Events.recentlyClosedTabsOpened.testHasValue())

        createInteractor().onOpenRecentlyClosedClicked()

        verify(exactly = 1) { navController.navigate(TabsTrayFragmentDirections.actionGlobalRecentlyClosed()) }
        assertTrue(Events.recentlyClosedTabsOpened.testHasValue())
    }

    @Test
    fun `onCloseAllTabsClicked calls navigation on DefaultNavigationInteractor`() {
        var dismissTabTrayAndNavigateHomeInvoked = false
        createInteractor(
            dismissTabTrayAndNavigateHome = {
                dismissTabTrayAndNavigateHomeInvoked = true
            }
        ).onCloseAllTabsClicked(false)

        assertTrue(dismissTabTrayAndNavigateHomeInvoked)
    }

    @Test
    fun `GIVEN active private download WHEN onCloseAllTabsClicked is called for private tabs THEN showCancelledDownloadWarning is called`() {
        var showCancelledDownloadWarningInvoked = false
        val mockedStore: BrowserStore = mockk()
        val controller = spyk(
            createInteractor(
                browserStore = mockedStore,
                showCancelledDownloadWarning = { _, _, _ ->
                    showCancelledDownloadWarningInvoked = true
                }
            )
        )
        val tab: TabSessionState = mockk { every { content.private } returns true }
        every { mockedStore.state } returns mockk()
        every { mockedStore.state.downloads } returns mapOf(
            "1" to DownloadState(
                "https://mozilla.org/download",
                private = true,
                destinationDirectory = "Download",
                status = DownloadState.Status.DOWNLOADING
            )
        )
        try {
            mockkStatic("mozilla.components.browser.state.selector.SelectorsKt")
            every { mockedStore.state.findTab(any()) } returns tab
            every { mockedStore.state.getNormalOrPrivateTabs(any()) } returns listOf(tab)

            controller.onCloseAllTabsClicked(true)

            assertTrue(showCancelledDownloadWarningInvoked)
        } finally {
            unmockkStatic("mozilla.components.browser.state.selector.SelectorsKt")
        }
    }

    @Test
    fun `onShareTabsOfType calls navigation on DefaultNavigationInteractor`() {
        createInteractor().onShareTabsOfTypeClicked(false)
        verify(exactly = 1) { navController.navigate(any<NavDirections>()) }
    }

    @Test
    fun `onShareTabs calls navigation on DefaultNavigationInteractor`() {

        createInteractor().onShareTabs(listOf(testTab))

        verify(exactly = 1) { navController.navigate(any<NavDirections>()) }

        assertTrue(TabsTray.shareSelectedTabs.testHasValue())
        val snapshot = TabsTray.shareSelectedTabs.testGetValue()
        Assert.assertEquals(1, snapshot.size)
        Assert.assertEquals("1", snapshot.single().extra?.getValue("tab_count"))
    }

    @Test
    fun `onSaveToCollections calls navigation on DefaultNavigationInteractor`() {
        mockkStatic("org.mozilla.fenix.collections.CollectionsDialogKt")

        every { any<CollectionsDialog>().show(any()) } answers { }
        assertFalse(TabsTray.saveToCollection.testHasValue())

        createInteractor().onSaveToCollections(listOf(testTab))

        assertTrue(TabsTray.saveToCollection.testHasValue())

        unmockkStatic("org.mozilla.fenix.collections.CollectionsDialogKt")
    }

    @Test
    fun `onBookmarkTabs calls navigation on DefaultNavigationInteractor`() = runBlockingTest {
        var showBookmarkSnackbarInvoked = false
        createInteractor(
            showBookmarkSnackbar = {
                showBookmarkSnackbarInvoked = true
            }
        ).onSaveToBookmarks(listOf(createStateTab("url")))

        coVerify(exactly = 1) { bookmarksUseCase.addBookmark(any(), any(), any()) }
        assertTrue(showBookmarkSnackbarInvoked)

        assertTrue(TabsTray.bookmarkSelectedTabs.testHasValue())
        val snapshot = TabsTray.bookmarkSelectedTabs.testGetValue()
        Assert.assertEquals(1, snapshot.size)
        Assert.assertEquals("1", snapshot.single().extra?.getValue("tab_count"))
    }

    @Test
    fun `onSyncedTabsClicked sets metrics and opens browser`() {
        val tab = mockk<SyncTab>()
        val entry = mockk<TabEntry>()
        assertFalse(Events.syncedTabOpened.testHasValue())

        every { tab.active() }.answers { entry }
        every { entry.url }.answers { "https://mozilla.org" }

        var dismissTabTrayInvoked = false
        createInteractor(
            dismissTabTray = {
                dismissTabTrayInvoked = true
            }
        ).onSyncedTabClicked(tab)

        assertTrue(dismissTabTrayInvoked)
        assertTrue(Events.syncedTabOpened.testHasValue())

        verify {
            activity.openToBrowserAndLoad(
                searchTermOrURL = "https://mozilla.org",
                newTab = true,
                from = BrowserDirection.FromTabsTray
            )
        }
    }

    @Suppress("LongParameterList")
    private fun createInteractor(
        browserStore: BrowserStore = store,
        dismissTabTray: () -> Unit = { },
        dismissTabTrayAndNavigateHome: (String) -> Unit = { _ -> },
        showCollectionSnackbar: (Int, Boolean, Long?) -> Unit = { _, _, _ -> },
        showBookmarkSnackbar: (Int) -> Unit = { _ -> },
        showCancelledDownloadWarning: (Int, String?, String?) -> Unit = { _, _, _ -> }
    ): NavigationInteractor {
        return DefaultNavigationInteractor(
            context,
            activity,
            browserStore,
            navController,
            metrics,
            dismissTabTray,
            dismissTabTrayAndNavigateHome,
            bookmarksUseCase,
            tabsTrayStore,
            collectionStorage,
            showCollectionSnackbar,
            showBookmarkSnackbar,
            showCancelledDownloadWarning,
            accountManager,
            testDispatcher
        )
    }
}
