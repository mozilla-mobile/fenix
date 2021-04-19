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
import io.mockk.verify
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.createTab as createStateTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.tabstray.Tab
import mozilla.components.service.fxa.manager.FxaAccountManager
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.collections.CollectionsDialog
import org.mozilla.fenix.collections.show
import org.mozilla.fenix.components.TabCollectionStorage
import org.mozilla.fenix.helpers.DisableNavGraphProviderAssertionRule
import org.mozilla.fenix.components.bookmarks.BookmarksUseCase
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.tabstray.browser.createTab as createTrayTab

class NavigationInteractorTest {
    private lateinit var store: BrowserStore
    private lateinit var tabsTrayStore: TabsTrayStore
    private lateinit var navigationInteractor: NavigationInteractor
    private val testTab: TabSessionState = createStateTab(url = "https://mozilla.org")
    private val navController: NavController = mockk(relaxed = true)
    private val metrics: MetricController = mockk(relaxed = true)
    private val dismissTabTray: () -> Unit = mockk(relaxed = true)
    private val dismissTabTrayAndNavigateHome: (String) -> Unit = mockk(relaxed = true)
    private val bookmarksUseCase: BookmarksUseCase = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val collectionStorage: TabCollectionStorage = mockk(relaxed = true)
    private val accountManager: FxaAccountManager = mockk(relaxed = true)

    @get:Rule
    val disableNavGraphProviderAssertionRule = DisableNavGraphProviderAssertionRule()

    @Before
    fun setup() {
        store = BrowserStore(initialState = BrowserState(tabs = listOf(testTab)))
        tabsTrayStore = TabsTrayStore()
        navigationInteractor = DefaultNavigationInteractor(
            context,
            store,
            navController,
            metrics,
            dismissTabTray,
            dismissTabTrayAndNavigateHome,
            bookmarksUseCase,
            tabsTrayStore,
            collectionStorage,
            accountManager
        )
    }

    @Test
    fun `navigation interactor calls the overridden functions`() {
        var tabTrayDismissed = false
        var accountSettingsClicked = false
        var tabSettingsClicked = false
        var openRecentlyClosedClicked = false
        var shareTabsOfTypeClicked = false
        var closeAllTabsClicked = false
        var onShareTabs = false
        var onSaveToCollections = false
        var onBookmarkTabs = false

        class TestNavigationInteractor : NavigationInteractor {

            override fun onTabTrayDismissed() {
                tabTrayDismissed = true
            }

            override fun onShareTabs(tabs: Collection<Tab>) {
                onShareTabs = true
            }

            override fun onAccountSettingsClicked() {
                accountSettingsClicked = true
            }

            override fun onTabSettingsClicked() {
                tabSettingsClicked = true
            }

            override fun onOpenRecentlyClosedClicked() {
                openRecentlyClosedClicked = true
            }

            override fun onSaveToCollections(tabs: Collection<Tab>) {
                onSaveToCollections = true
            }

            override fun onSaveToBookmarks(tabs: Collection<Tab>) {
                onBookmarkTabs = true
            }

            override fun onShareTabsOfTypeClicked(private: Boolean) {
                shareTabsOfTypeClicked = true
            }

            override fun onCloseAllTabsClicked(private: Boolean) {
                closeAllTabsClicked = true
            }
        }

        val navigationInteractor: NavigationInteractor = TestNavigationInteractor()
        navigationInteractor.onTabTrayDismissed()
        assertTrue(tabTrayDismissed)
        navigationInteractor.onAccountSettingsClicked()
        assertTrue(accountSettingsClicked)
        navigationInteractor.onTabSettingsClicked()
        assertTrue(tabSettingsClicked)
        navigationInteractor.onOpenRecentlyClosedClicked()
        assertTrue(openRecentlyClosedClicked)
        navigationInteractor.onShareTabsOfTypeClicked(true)
        assertTrue(shareTabsOfTypeClicked)
        navigationInteractor.onCloseAllTabsClicked(true)
        assertTrue(closeAllTabsClicked)
        navigationInteractor.onShareTabs(emptyList())
        assertTrue(onShareTabs)
        navigationInteractor.onSaveToCollections(emptyList())
        assertTrue(onSaveToCollections)
        navigationInteractor.onSaveToBookmarks(emptyList())
        assertTrue(onBookmarkTabs)
    }

    @Test
    fun `onTabTrayDismissed calls dismissTabTray on DefaultNavigationInteractor`() {
        navigationInteractor.onTabTrayDismissed()
        verify(exactly = 1) { dismissTabTray() }
    }

    @Test
    fun `onAccountSettingsClicked calls navigation on DefaultNavigationInteractor`() {
        every { accountManager.authenticatedAccount() }.answers { mockk(relaxed = true) }

        navigationInteractor.onAccountSettingsClicked()

        verify(exactly = 1) { navController.navigate(TabsTrayFragmentDirections.actionGlobalAccountSettingsFragment()) }
    }

    @Test
    fun `onAccountSettingsClicked when not logged in calls navigation to turn on sync`() {
        every { accountManager.authenticatedAccount() }.answers { null }

        navigationInteractor.onAccountSettingsClicked()

        verify(exactly = 1) { navController.navigate(TabsTrayFragmentDirections.actionGlobalTurnOnSync()) }
    }

    @Test
    fun `onTabSettingsClicked calls navigation on DefaultNavigationInteractor`() {
        navigationInteractor.onTabSettingsClicked()
        verify(exactly = 1) { navController.navigate(TabsTrayFragmentDirections.actionGlobalTabSettingsFragment()) }
    }

    @Test
    fun `onOpenRecentlyClosedClicked calls navigation on DefaultNavigationInteractor`() {
        navigationInteractor.onOpenRecentlyClosedClicked()
        verify(exactly = 1) { navController.navigate(TabsTrayFragmentDirections.actionGlobalRecentlyClosed()) }
    }

    @Test
    fun `onCloseAllTabsClicked calls navigation on DefaultNavigationInteractor`() {
        navigationInteractor.onCloseAllTabsClicked(false)
        verify(exactly = 1) { dismissTabTrayAndNavigateHome(any()) }
    }

    @Test
    fun `onShareTabsOfType calls navigation on DefaultNavigationInteractor`() {
        navigationInteractor.onShareTabsOfTypeClicked(false)
        verify(exactly = 1) { navController.navigate(any<NavDirections>()) }
    }

    @Test
    fun `onShareTabs calls navigation on DefaultNavigationInteractor`() {
        navigationInteractor.onShareTabs(emptyList())
        verify(exactly = 1) { navController.navigate(any<NavDirections>()) }
    }

    @Test
    fun `onSaveToCollections calls navigation on DefaultNavigationInteractor`() {
        mockkStatic("org.mozilla.fenix.collections.CollectionsDialogKt")

        every { any<CollectionsDialog>().show(any()) } answers { }
        navigationInteractor.onSaveToCollections(emptyList())
        verify(exactly = 1) { metrics.track(Event.TabsTraySaveToCollectionPressed) }

        unmockkStatic("org.mozilla.fenix.collections.CollectionsDialogKt")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `onBookmarkTabs calls navigation on DefaultNavigationInteractor`() = runBlockingTest {
        navigationInteractor.onSaveToBookmarks(listOf(createTrayTab()))
        coVerify(exactly = 1) { bookmarksUseCase.addBookmark(any(), any(), any()) }
    }
}
