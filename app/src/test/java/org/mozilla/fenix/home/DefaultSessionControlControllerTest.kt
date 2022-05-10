/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home

import androidx.navigation.NavController
import androidx.navigation.NavDirections
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mozilla.components.browser.state.action.SearchAction
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.search.RegionState
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.ReaderState
import mozilla.components.browser.state.state.SearchState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.state.recover.RecoverableTab
import mozilla.components.browser.state.state.recover.TabState
import mozilla.components.browser.state.state.selectedOrDefaultSearchEngine
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tab.collections.TabCollection
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.service.glean.testing.GleanTestRule
import mozilla.components.support.test.ext.joinBlocking
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.test.rule.MainCoroutineRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.GleanMetrics.Collections
import org.mozilla.fenix.GleanMetrics.Events
import org.mozilla.fenix.GleanMetrics.HomeScreen
import org.mozilla.fenix.GleanMetrics.Pings
import org.mozilla.fenix.GleanMetrics.RecentTabs
import org.mozilla.fenix.GleanMetrics.RecentBookmarks
import org.mozilla.fenix.GleanMetrics.TopSites
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.BrowserFragmentDirections
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.Analytics
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.TabCollectionStorage
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.gleanplumb.Message
import org.mozilla.fenix.gleanplumb.MessageController
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.home.recentbookmarks.RecentBookmark
import org.mozilla.fenix.home.recenttabs.RecentTab
import org.mozilla.fenix.home.sessioncontrol.DefaultSessionControlController
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.utils.Settings
import mozilla.components.feature.tab.collections.Tab as ComponentTab

@RunWith(FenixRobolectricTestRunner::class) // For gleanTestRule
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultSessionControlControllerTest {

    @get:Rule
    val coroutinesTestRule = MainCoroutineRule()
    @get:Rule
    val gleanTestRule = GleanTestRule(testContext)

    private val activity: HomeActivity = mockk(relaxed = true)
    private val appStore: AppStore = mockk(relaxed = true)
    private val navController: NavController = mockk(relaxed = true)
    private val messageController: MessageController = mockk(relaxed = true)
    private val engine: Engine = mockk(relaxed = true)
    private val tabCollectionStorage: TabCollectionStorage = mockk(relaxed = true)
    private val tabsUseCases: TabsUseCases = mockk(relaxed = true)
    private val reloadUrlUseCase: SessionUseCases = mockk(relaxed = true)
    private val selectTabUseCase: TabsUseCases = mockk(relaxed = true)
    private val settings: Settings = mockk(relaxed = true)
    private val analytics: Analytics = mockk(relaxed = true)
    private val scope = coroutinesTestRule.scope
    private val searchEngine = SearchEngine(
        id = "test",
        name = "Test Engine",
        icon = mockk(relaxed = true),
        type = SearchEngine.Type.BUNDLED,
        resultUrls = listOf("https://example.org/?q={searchTerms}")
    )

    private val googleSearchEngine = SearchEngine(
        id = "googleTest",
        name = "Google Test Engine",
        icon = mockk(relaxed = true),
        type = SearchEngine.Type.BUNDLED,
        resultUrls = listOf("https://www.google.com/?q={searchTerms}"),
        suggestUrl = "https://www.google.com/"
    )

    private val duckDuckGoSearchEngine = SearchEngine(
        id = "ddgTest",
        name = "DuckDuckGo Test Engine",
        icon = mockk(relaxed = true),
        type = SearchEngine.Type.BUNDLED,
        resultUrls = listOf("https://duckduckgo.com/?q=%7BsearchTerms%7D&t=fpas"),
        suggestUrl = "https://ac.duckduckgo.com/ac/?q=%7BsearchTerms%7D&type=list"
    )

    private lateinit var store: BrowserStore
    private val appState: AppState = mockk(relaxed = true)

    @Before
    fun setup() {
        store = BrowserStore(
            BrowserState(
                search = SearchState(
                    regionSearchEngines = listOf(searchEngine)
                )
            )
        )

        every { appStore.state } returns AppState(
            collections = emptyList(),
            expandedCollections = emptySet(),
            mode = Mode.Normal,
            topSites = emptyList(),
            showCollectionPlaceholder = true,
            recentTabs = emptyList(),
            recentBookmarks = emptyList()
        )

        every { navController.currentDestination } returns mockk {
            every { id } returns R.id.homeFragment
        }
        every { activity.components.settings } returns settings
        every { activity.settings() } returns settings
        every { activity.components.analytics } returns analytics
    }

    @Test
    fun handleCollectionAddTabTapped() {
        val collection = mockk<TabCollection> {
            every { id } returns 12L
        }
        createController().handleCollectionAddTabTapped(collection)

        assertTrue(Collections.addTabButton.testHasValue())
        val recordedEvents = Collections.addTabButton.testGetValue()
        assertEquals(1, recordedEvents.size)
        assertEquals(null, recordedEvents.single().extra)

        verify {
            navController.navigate(
                match<NavDirections> {
                    it.actionId == R.id.action_global_collectionCreationFragment
                },
                null
            )
        }
    }

    @Test
    fun handleCustomizeHomeTapped() {
        assertFalse(HomeScreen.customizeHomeClicked.testHasValue())

        createController().handleCustomizeHomeTapped()

        assertTrue(HomeScreen.customizeHomeClicked.testHasValue())
        verify {
            navController.navigate(
                match<NavDirections> {
                    it.actionId == R.id.action_global_homeSettingsFragment
                },
                null
            )
        }
    }

    @Test
    @Ignore("Until the feature is enabled again")
    fun handleShowOnboardingDialog() {
        createController().handleShowOnboardingDialog()

        verify {
            navController.navigate(
                match<NavDirections> {
                    it.actionId == R.id.action_global_home_onboarding_dialog
                },
                null
            )
        }
    }

    @Test
    fun `handleCollectionOpenTabClicked onFailure`() {
        val tab = mockk<ComponentTab> {
            every { url } returns "https://mozilla.org"
            every { restore(activity, engine, restoreSessionId = false) } returns null
        }
        createController().handleCollectionOpenTabClicked(tab)

        assertTrue(Collections.tabRestored.testHasValue())
        val recordedEvents = Collections.tabRestored.testGetValue()
        assertEquals(1, recordedEvents.size)
        assertEquals(null, recordedEvents.single().extra)

        verify {
            activity.openToBrowserAndLoad(
                searchTermOrURL = "https://mozilla.org",
                newTab = true,
                from = BrowserDirection.FromHome
            )
        }
    }

    @Test
    fun `handleCollectionOpenTabClicked with existing selected tab`() {
        val recoverableTab = RecoverableTab(
            engineSessionState = null,
            state = TabState(
                id = "test",
                parentId = null,
                url = "https://www.mozilla.org",
                title = "Mozilla",
                contextId = null,
                readerState = ReaderState(),
                lastAccess = 0,
                private = false
            )
        )

        val tab = mockk<ComponentTab> {
            every { restore(activity, engine, restoreSessionId = false) } returns recoverableTab
        }

        val restoredTab = createTab(id = recoverableTab.state.id, url = recoverableTab.state.url)
        val otherTab = createTab(id = "otherTab", url = "https://mozilla.org")
        store.dispatch(TabListAction.AddTabAction(otherTab)).joinBlocking()
        store.dispatch(TabListAction.SelectTabAction(otherTab.id)).joinBlocking()
        store.dispatch(TabListAction.AddTabAction(restoredTab)).joinBlocking()

        createController().handleCollectionOpenTabClicked(tab)

        assertTrue(Collections.tabRestored.testHasValue())
        val recordedEvents = Collections.tabRestored.testGetValue()
        assertEquals(1, recordedEvents.size)
        assertEquals(null, recordedEvents.single().extra)

        verify { activity.openToBrowser(BrowserDirection.FromHome) }
        verify { selectTabUseCase.selectTab.invoke(restoredTab.id) }
        verify { reloadUrlUseCase.reload.invoke(restoredTab.id) }
    }

    @Test
    fun `handleCollectionOpenTabClicked without existing selected tab`() {
        val recoverableTab = RecoverableTab(
            engineSessionState = null,
            state = TabState(
                id = "test",
                parentId = null,
                url = "https://www.mozilla.org",
                title = "Mozilla",
                contextId = null,
                readerState = ReaderState(),
                lastAccess = 0,
                private = false
            )
        )

        val tab = mockk<ComponentTab> {
            every { restore(activity, engine, restoreSessionId = false) } returns recoverableTab
        }

        val restoredTab = createTab(id = recoverableTab.state.id, url = recoverableTab.state.url)
        store.dispatch(TabListAction.AddTabAction(restoredTab)).joinBlocking()

        createController().handleCollectionOpenTabClicked(tab)

        assertTrue(Collections.tabRestored.testHasValue())
        val recordedEvents = Collections.tabRestored.testGetValue()
        assertEquals(1, recordedEvents.size)
        assertEquals(null, recordedEvents.single().extra)

        verify { activity.openToBrowser(BrowserDirection.FromHome) }
        verify { selectTabUseCase.selectTab.invoke(restoredTab.id) }
        verify { reloadUrlUseCase.reload.invoke(restoredTab.id) }
    }

    @Test
    fun handleCollectionOpenTabsTapped() {
        val collection = mockk<TabCollection> {
            every { tabs } returns emptyList()
        }
        createController().handleCollectionOpenTabsTapped(collection)

        assertTrue(Collections.allTabsRestored.testHasValue())
        val recordedEvents = Collections.allTabsRestored.testGetValue()
        assertEquals(1, recordedEvents.size)
        assertEquals(null, recordedEvents.single().extra)
    }

    @Test
    fun `handleCollectionRemoveTab one tab`() {
        val expectedCollection = mockk<TabCollection> {
            every { tabs } returns listOf(mockk())
            every { title } returns "Collection"
        }
        val tab = mockk<ComponentTab>()
        every {
            activity.resources.getString(
                R.string.delete_tab_and_collection_dialog_title,
                "Collection"
            )
        } returns "Delete Collection?"
        every {
            activity.resources.getString(R.string.delete_tab_and_collection_dialog_message)
        } returns "Deleting this tab will delete everything."

        var actualCollection: TabCollection? = null

        createController(
            removeCollectionWithUndo = { collection ->
                actualCollection = collection
            }
        ).handleCollectionRemoveTab(expectedCollection, tab, false)

        assertTrue(Collections.tabRemoved.testHasValue())
        val recordedEvents = Collections.tabRemoved.testGetValue()
        assertEquals(1, recordedEvents.size)
        assertEquals(null, recordedEvents.single().extra)

        assertEquals(expectedCollection, actualCollection)
    }

    @Test
    fun `handleCollectionRemoveTab multiple tabs`() {
        val collection: TabCollection = mockk(relaxed = true)
        val tab: ComponentTab = mockk(relaxed = true)
        createController().handleCollectionRemoveTab(collection, tab, false)

        assertTrue(Collections.tabRemoved.testHasValue())
        val recordedEvents = Collections.tabRemoved.testGetValue()
        assertEquals(1, recordedEvents.size)
        assertEquals(null, recordedEvents.single().extra)
    }

    @Test
    fun handleCollectionShareTabsClicked() {
        val collection = mockk<TabCollection> {
            every { tabs } returns emptyList()
            every { title } returns ""
        }
        createController().handleCollectionShareTabsClicked(collection)

        assertTrue(Collections.shared.testHasValue())
        val recordedEvents = Collections.shared.testGetValue()
        assertEquals(1, recordedEvents.size)
        assertEquals(null, recordedEvents.single().extra)

        verify {
            navController.navigate(
                match<NavDirections> { it.actionId == R.id.action_global_shareFragment },
                null
            )
        }
    }

    @Test
    fun handleDeleteCollectionTapped() {
        val expectedCollection = mockk<TabCollection> {
            every { title } returns "Collection"
        }
        every {
            activity.resources.getString(R.string.tab_collection_dialog_message, "Collection")
        } returns "Are you sure you want to delete Collection?"

        var actualCollection: TabCollection? = null

        createController(
            removeCollectionWithUndo = { collection ->
                actualCollection = collection
            }
        ).handleDeleteCollectionTapped(expectedCollection)

        assertEquals(expectedCollection, actualCollection)
        assertTrue(Collections.removed.testHasValue())
        val recordedEvents = Collections.removed.testGetValue()
        assertEquals(1, recordedEvents.size)
        assertEquals(null, recordedEvents.single().extra)
    }

    @Test
    fun handlePrivateBrowsingLearnMoreClicked() {
        createController().handlePrivateBrowsingLearnMoreClicked()
        verify {
            activity.openToBrowserAndLoad(
                searchTermOrURL = SupportUtils.getGenericSumoURLForTopic
                (SupportUtils.SumoTopic.PRIVATE_BROWSING_MYTHS),
                newTab = true,
                from = BrowserDirection.FromHome
            )
        }
    }

    @Test
    fun handleRenameCollectionTapped() {
        val collection = mockk<TabCollection> {
            every { id } returns 3L
        }
        createController().handleRenameCollectionTapped(collection)

        assertTrue(Collections.renameButton.testHasValue())
        val recordedEvents = Collections.renameButton.testGetValue()
        assertEquals(1, recordedEvents.size)
        assertEquals(null, recordedEvents.single().extra)

        verify {
            navController.navigate(
                match<NavDirections> { it.actionId == R.id.action_global_collectionCreationFragment },
                null
            )
        }
    }

    @Test
    fun handleSelectDefaultTopSite() {
        val topSite = TopSite.Default(
            id = 1L,
            title = "Mozilla",
            url = "mozilla.org",
            createdAt = 0
        )
        val controller = spyk(createController())

        every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

        controller.handleSelectTopSite(topSite, position = 0)

        assertTrue(TopSites.openInNewTab.testHasValue())
        assertEquals(1, TopSites.openInNewTab.testGetValue().size)
        assertNull(TopSites.openInNewTab.testGetValue().single().extra)

        assertTrue(TopSites.openDefault.testHasValue())
        assertEquals(1, TopSites.openDefault.testGetValue().size)
        assertNull(TopSites.openDefault.testGetValue().single().extra)

        verify {
            tabsUseCases.addTab.invoke(
                url = topSite.url,
                selectTab = true,
                startLoading = true
            )
        }
        verify { activity.openToBrowser(BrowserDirection.FromHome) }
    }

    @Test
    fun handleSelectNonDefaultTopSite() {
        val topSite = TopSite.Frecent(
            id = 1L,
            title = "Mozilla",
            url = "mozilla.org",
            createdAt = 0
        )
        val controller = spyk(createController())

        every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

        controller.handleSelectTopSite(topSite, position = 0)

        assertTrue(TopSites.openInNewTab.testHasValue())
        assertEquals(1, TopSites.openInNewTab.testGetValue().size)
        assertNull(TopSites.openInNewTab.testGetValue().single().extra)

        verify {
            tabsUseCases.addTab.invoke(
                url = topSite.url,
                selectTab = true,
                startLoading = true
            )
        }
        verify { activity.openToBrowser(BrowserDirection.FromHome) }
    }

    @Test
    fun handleSelectGoogleDefaultTopSiteUS() {
        val topSite = TopSite.Default(
            id = 1L,
            title = "Google",
            url = SupportUtils.GOOGLE_URL,
            createdAt = 0
        )
        val controller = spyk(createController())

        every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

        store.dispatch(SearchAction.SetRegionAction(RegionState("US", "US"))).joinBlocking()

        controller.handleSelectTopSite(topSite, position = 0)

        assertTrue(TopSites.openInNewTab.testHasValue())
        assertEquals(1, TopSites.openInNewTab.testGetValue().size)
        assertNull(TopSites.openInNewTab.testGetValue().single().extra)

        assertTrue(TopSites.openDefault.testHasValue())
        assertEquals(1, TopSites.openDefault.testGetValue().size)
        assertNull(TopSites.openDefault.testGetValue().single().extra)

        assertTrue(TopSites.openGoogleSearchAttribution.testHasValue())
        assertEquals(1, TopSites.openGoogleSearchAttribution.testGetValue().size)
        assertNull(TopSites.openGoogleSearchAttribution.testGetValue().single().extra)

        verify {
            tabsUseCases.addTab.invoke(
                url = SupportUtils.GOOGLE_US_URL,
                selectTab = true,
                startLoading = true
            )
        }
        verify { activity.openToBrowser(BrowserDirection.FromHome) }
    }

    @Test
    fun handleSelectGoogleDefaultTopSiteXX() {
        val topSite = TopSite.Default(
            id = 1L,
            title = "Google",
            url = SupportUtils.GOOGLE_URL,
            createdAt = 0
        )
        val controller = spyk(createController())

        every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

        store.dispatch(SearchAction.SetRegionAction(RegionState("DE", "FR"))).joinBlocking()

        controller.handleSelectTopSite(topSite, position = 0)

        assertTrue(TopSites.openInNewTab.testHasValue())
        assertEquals(1, TopSites.openInNewTab.testGetValue().size)
        assertNull(TopSites.openInNewTab.testGetValue().single().extra)

        assertTrue(TopSites.openDefault.testHasValue())
        assertEquals(1, TopSites.openDefault.testGetValue().size)
        assertNull(TopSites.openDefault.testGetValue().single().extra)

        assertTrue(TopSites.openGoogleSearchAttribution.testHasValue())
        assertEquals(1, TopSites.openGoogleSearchAttribution.testGetValue().size)
        assertNull(TopSites.openGoogleSearchAttribution.testGetValue().single().extra)

        verify {
            tabsUseCases.addTab.invoke(
                SupportUtils.GOOGLE_XX_URL,
                selectTab = true,
                startLoading = true
            )
        }
        verify { activity.openToBrowser(BrowserDirection.FromHome) }
    }

    @Test
    fun handleSelectGoogleDefaultTopSite_EventPerformedSearchTopSite() {
        assertFalse(Events.performedSearch.testHasValue())

        val topSite = TopSite.Default(
            id = 1L,
            title = "Google",
            url = SupportUtils.GOOGLE_URL,
            createdAt = 0
        )
        val controller = spyk(createController())

        every { controller.getAvailableSearchEngines() } returns listOf(googleSearchEngine)

        try {
            mockkStatic("mozilla.components.browser.state.state.SearchStateKt")

            every { any<SearchState>().selectedOrDefaultSearchEngine } returns googleSearchEngine

            controller.handleSelectTopSite(topSite, position = 0)

            assertTrue(Events.performedSearch.testHasValue())

            assertTrue(TopSites.openDefault.testHasValue())
            assertEquals(1, TopSites.openDefault.testGetValue().size)
            assertNull(TopSites.openDefault.testGetValue().single().extra)

            assertTrue(TopSites.openGoogleSearchAttribution.testHasValue())
            assertEquals(1, TopSites.openGoogleSearchAttribution.testGetValue().size)
            assertNull(TopSites.openGoogleSearchAttribution.testGetValue().single().extra)
        } finally {
            unmockkStatic("mozilla.components.browser.state.state.SearchStateKt")
        }
    }

    @Test
    fun handleSelectDuckDuckGoTopSite_EventPerformedSearchTopSite() {
        assertFalse(Events.performedSearch.testHasValue())

        val topSite = TopSite.Pinned(
            id = 1L,
            title = "DuckDuckGo",
            url = "https://duckduckgo.com",
            createdAt = 0
        )
        val controller = spyk(createController())

        every { controller.getAvailableSearchEngines() } returns listOf(googleSearchEngine, duckDuckGoSearchEngine)

        try {
            mockkStatic("mozilla.components.browser.state.state.SearchStateKt")

            every { any<SearchState>().selectedOrDefaultSearchEngine } returns googleSearchEngine

            controller.handleSelectTopSite(topSite, position = 0)

            assertTrue(Events.performedSearch.testHasValue())
        } finally {
            unmockkStatic("mozilla.components.browser.state.state.SearchStateKt")
        }
    }

    @Test
    fun handleSelectGooglePinnedTopSiteUS() {
        val topSite = TopSite.Pinned(
            id = 1L,
            title = "Google",
            url = SupportUtils.GOOGLE_URL,
            createdAt = 0
        )
        val controller = spyk(createController())

        every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

        store.dispatch(SearchAction.SetRegionAction(RegionState("US", "US"))).joinBlocking()

        controller.handleSelectTopSite(topSite, position = 0)

        assertTrue(TopSites.openInNewTab.testHasValue())
        assertEquals(1, TopSites.openInNewTab.testGetValue().size)
        assertNull(TopSites.openInNewTab.testGetValue().single().extra)

        assertTrue(TopSites.openPinned.testHasValue())
        assertEquals(1, TopSites.openPinned.testGetValue().size)
        assertNull(TopSites.openPinned.testGetValue().single().extra)

        assertTrue(TopSites.openGoogleSearchAttribution.testHasValue())
        assertEquals(1, TopSites.openGoogleSearchAttribution.testGetValue().size)
        assertNull(TopSites.openGoogleSearchAttribution.testGetValue().single().extra)

        verify {
            tabsUseCases.addTab.invoke(
                SupportUtils.GOOGLE_US_URL,
                selectTab = true,
                startLoading = true
            )
        }
        verify { activity.openToBrowser(BrowserDirection.FromHome) }
    }

    @Test
    fun handleSelectGooglePinnedTopSiteXX() {
        val topSite = TopSite.Pinned(
            id = 1L,
            title = "Google",
            url = SupportUtils.GOOGLE_URL,
            createdAt = 0
        )
        val controller = spyk(createController())

        every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

        store.dispatch(SearchAction.SetRegionAction(RegionState("DE", "FR"))).joinBlocking()

        controller.handleSelectTopSite(topSite, position = 0)

        assertTrue(TopSites.openInNewTab.testHasValue())
        assertEquals(1, TopSites.openInNewTab.testGetValue().size)
        assertNull(TopSites.openInNewTab.testGetValue().single().extra)

        assertTrue(TopSites.openPinned.testHasValue())
        assertEquals(1, TopSites.openPinned.testGetValue().size)
        assertNull(TopSites.openPinned.testGetValue().single().extra)

        assertTrue(TopSites.openGoogleSearchAttribution.testHasValue())
        assertEquals(1, TopSites.openGoogleSearchAttribution.testGetValue().size)
        assertNull(TopSites.openGoogleSearchAttribution.testGetValue().single().extra)

        verify {
            tabsUseCases.addTab.invoke(
                SupportUtils.GOOGLE_XX_URL,
                selectTab = true,
                startLoading = true
            )
        }
        verify { activity.openToBrowser(BrowserDirection.FromHome) }
    }

    @Test
    fun handleSelectGoogleFrecentTopSiteUS() {
        val topSite = TopSite.Frecent(
            id = 1L,
            title = "Google",
            url = SupportUtils.GOOGLE_URL,
            createdAt = 0
        )
        val controller = spyk(createController())

        every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

        store.dispatch(SearchAction.SetRegionAction(RegionState("US", "US"))).joinBlocking()

        controller.handleSelectTopSite(topSite, position = 0)

        assertTrue(TopSites.openInNewTab.testHasValue())
        assertEquals(1, TopSites.openInNewTab.testGetValue().size)
        assertNull(TopSites.openInNewTab.testGetValue().single().extra)

        assertTrue(TopSites.openFrecency.testHasValue())
        assertEquals(1, TopSites.openFrecency.testGetValue().size)
        assertNull(TopSites.openFrecency.testGetValue().single().extra)

        assertTrue(TopSites.openGoogleSearchAttribution.testHasValue())
        assertEquals(1, TopSites.openGoogleSearchAttribution.testGetValue().size)
        assertNull(TopSites.openGoogleSearchAttribution.testGetValue().single().extra)

        verify {
            tabsUseCases.addTab.invoke(
                SupportUtils.GOOGLE_US_URL,
                selectTab = true,
                startLoading = true
            )
        }
        verify { activity.openToBrowser(BrowserDirection.FromHome) }
    }

    @Test
    fun handleSelectGoogleFrecentTopSiteXX() {
        val topSite = TopSite.Frecent(
            id = 1L,
            title = "Google",
            url = SupportUtils.GOOGLE_URL,
            createdAt = 0
        )
        val controller = spyk(createController())

        every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

        store.dispatch(SearchAction.SetRegionAction(RegionState("DE", "FR"))).joinBlocking()

        controller.handleSelectTopSite(topSite, position = 0)

        assertTrue(TopSites.openInNewTab.testHasValue())
        assertEquals(1, TopSites.openInNewTab.testGetValue().size)
        assertNull(TopSites.openInNewTab.testGetValue().single().extra)

        assertTrue(TopSites.openFrecency.testHasValue())
        assertEquals(1, TopSites.openFrecency.testGetValue().size)
        assertNull(TopSites.openFrecency.testGetValue().single().extra)

        assertTrue(TopSites.openGoogleSearchAttribution.testHasValue())
        assertEquals(1, TopSites.openGoogleSearchAttribution.testGetValue().size)
        assertNull(TopSites.openGoogleSearchAttribution.testGetValue().single().extra)

        verify {
            tabsUseCases.addTab.invoke(
                SupportUtils.GOOGLE_XX_URL,
                selectTab = true,
                startLoading = true
            )
        }
        verify { activity.openToBrowser(BrowserDirection.FromHome) }
    }

    @Test
    fun handleSelectProvidedTopSite() {
        val topSite = TopSite.Provided(
            id = 1L,
            title = "Mozilla",
            url = "mozilla.org",
            clickUrl = "",
            imageUrl = "",
            impressionUrl = "",
            createdAt = 0
        )
        val position = 0
        val controller = spyk(createController())

        every { controller.getAvailableSearchEngines() } returns listOf(searchEngine)

        controller.handleSelectTopSite(topSite, position)

        assertTrue(TopSites.openInNewTab.testHasValue())
        assertEquals(1, TopSites.openInNewTab.testGetValue().size)
        assertNull(TopSites.openInNewTab.testGetValue().single().extra)

        assertTrue(TopSites.openContileTopSite.testHasValue())
        assertEquals(1, TopSites.openContileTopSite.testGetValue().size)
        assertNull(TopSites.openContileTopSite.testGetValue().single().extra)

        verify {
            tabsUseCases.addTab.invoke(
                url = topSite.url,
                selectTab = true,
                startLoading = true
            )
        }
        verify { controller.submitTopSitesImpressionPing(topSite, position) }
        verify { activity.openToBrowser(BrowserDirection.FromHome) }
    }

    @Test
    fun `GIVEN a provided top site WHEN the provided top site is clicked THEN submit a top site impression ping`() {
        val controller = spyk(createController())
        val topSite = TopSite.Provided(
            id = 3,
            title = "Mozilla",
            url = "https://mozilla.com",
            clickUrl = "https://mozilla.com/click",
            imageUrl = "https://test.com/image2.jpg",
            impressionUrl = "https://example.com",
            createdAt = 3
        )
        val position = 0
        assertFalse(TopSites.contileImpression.testHasValue())

        var topSiteImpressionPinged = false
        Pings.topsitesImpression.testBeforeNextSubmit {
            assertTrue(TopSites.contileTileId.testHasValue())
            assertEquals(3, TopSites.contileTileId.testGetValue())

            assertTrue(TopSites.contileAdvertiser.testHasValue())
            assertEquals("mozilla", TopSites.contileAdvertiser.testGetValue())

            assertTrue(TopSites.contileReportingUrl.testHasValue())
            assertEquals(topSite.clickUrl, TopSites.contileReportingUrl.testGetValue())

            topSiteImpressionPinged = true
        }

        controller.submitTopSitesImpressionPing(topSite, position)

        assertTrue(TopSites.contileClick.testHasValue())

        val event = TopSites.contileClick.testGetValue()

        assertEquals(1, event.size)
        assertEquals("top_sites", event[0].category)
        assertEquals("contile_click", event[0].name)
        assertEquals("1", event[0].extra!!["position"])
        assertEquals("newtab", event[0].extra!!["source"])

        assertTrue(topSiteImpressionPinged)
    }

    @Test
    fun `WHEN the default Google top site is removed THEN the correct metric is recorded`() {
        val controller = spyk(createController())
        val topSite = TopSite.Default(
            id = 1L,
            title = "Google",
            url = SupportUtils.GOOGLE_URL,
            createdAt = 0
        )
        assertFalse(TopSites.remove.testHasValue())
        assertFalse(TopSites.googleTopSiteRemoved.testHasValue())

        controller.handleRemoveTopSiteClicked(topSite)

        assertTrue(TopSites.googleTopSiteRemoved.testHasValue())
        assertEquals(1, TopSites.googleTopSiteRemoved.testGetValue().size)
        assertNull(TopSites.googleTopSiteRemoved.testGetValue().single().extra)

        assertTrue(TopSites.remove.testHasValue())
        assertEquals(1, TopSites.remove.testGetValue().size)
        assertNull(TopSites.remove.testGetValue().single().extra)
    }

    @Test
    fun handleStartBrowsingClicked() {
        var hideOnboardingInvoked = false
        createController(hideOnboarding = { hideOnboardingInvoked = true }).handleStartBrowsingClicked()

        assertTrue(hideOnboardingInvoked)
    }

    @Test
    fun handleReadPrivacyNoticeClicked() {
        createController().handleReadPrivacyNoticeClicked()
        verify {
            activity.openToBrowserAndLoad(
                searchTermOrURL = SupportUtils.getMozillaPageUrl(SupportUtils.MozillaPage.PRIVATE_NOTICE),
                newTab = true,
                from = BrowserDirection.FromHome
            )
        }
    }

    @Test
    fun handleToggleCollectionExpanded() {
        val collection = mockk<TabCollection>()
        createController().handleToggleCollectionExpanded(collection, true)
        verify { appStore.dispatch(AppAction.CollectionExpanded(collection, true)) }
    }

    @Test
    fun handleCreateCollection() {
        createController().handleCreateCollection()

        verify {
            navController.navigate(
                match<NavDirections> { it.actionId == R.id.action_global_tabsTrayFragment },
                null
            )
        }
    }

    @Test
    fun handlePasteAndGo() {
        assertFalse(Events.enteredUrl.testHasValue())
        assertFalse(Events.performedSearch.testHasValue())

        createController().handlePasteAndGo("text")

        verify {
            activity.openToBrowserAndLoad(
                searchTermOrURL = "text",
                newTab = true,
                from = BrowserDirection.FromHome,
                engine = searchEngine
            )
        }

        assertTrue(Events.performedSearch.testHasValue())

        createController().handlePasteAndGo("https://mozilla.org")
        verify {
            activity.openToBrowserAndLoad(
                searchTermOrURL = "https://mozilla.org",
                newTab = true,
                from = BrowserDirection.FromHome,
                engine = searchEngine
            )
        }
        assertTrue(Events.enteredUrl.testHasValue())
    }

    @Test
    fun handlePaste() {
        createController().handlePaste("text")

        verify {
            navController.navigate(
                match<NavDirections> { it.actionId == R.id.action_global_search_dialog },
                null
            )
        }
    }

    @Test
    fun handleRemoveCollectionsPlaceholder() {
        createController().handleRemoveCollectionsPlaceholder()

        verify {
            settings.showCollectionsPlaceholderOnHome = false
            appStore.dispatch(AppAction.RemoveCollectionsPlaceholder)
        }
    }

    @Test
    @Ignore("Can't instantiate proxy for class kotlin.Function0")
    fun handleMenuOpenedWhileSearchShowing() {
        every { navController.currentDestination } returns mockk {
            every { id } returns R.id.searchDialogFragment
        }

        createController().handleMenuOpened()

        verify {
            navController.navigateUp()
        }
    }

    @Test
    fun handleMenuOpenedWhileSearchNotShowing() {
        every { navController.currentDestination } returns mockk {
            every { id } returns R.id.homeFragment
        }

        createController().handleMenuOpened()

        verify(exactly = 0) {
            navController.navigateUp()
        }
    }

    @Test
    fun `WHEN private mode button is selected from home THEN handle mode change`() {
        every { navController.currentDestination } returns mockk {
            every { id } returns R.id.homeFragment
        }

        every { settings.incrementNumTimesPrivateModeOpened() } just Runs

        val newMode = BrowsingMode.Private
        val hasBeenOnboarded = true

        createController().handlePrivateModeButtonClicked(newMode, hasBeenOnboarded)

        verify {
            settings.incrementNumTimesPrivateModeOpened()
            AppAction.ModeChange(Mode.fromBrowsingMode(newMode))
        }
    }

    @Test
    fun `WHEN private mode is selected on home from behind search THEN handle mode change`() {
        every { navController.currentDestination } returns mockk {
            every { id } returns R.id.searchDialogFragment
        }

        every { settings.incrementNumTimesPrivateModeOpened() } just Runs

        val url = "https://mozilla.org"
        val tab = createTab(
            id = "otherTab",
            url = url,
            private = false,
            engineSession = mockk(relaxed = true)
        )
        store.dispatch(TabListAction.AddTabAction(tab, select = true)).joinBlocking()

        val newMode = BrowsingMode.Private
        val hasBeenOnboarded = true

        createController().handlePrivateModeButtonClicked(newMode, hasBeenOnboarded)

        verify {
            settings.incrementNumTimesPrivateModeOpened()
            AppAction.ModeChange(Mode.fromBrowsingMode(newMode))
            navController.navigate(
                BrowserFragmentDirections.actionGlobalSearchDialog(
                    sessionId = null
                )
            )
        }
    }

    @Test
    fun `WHEN private mode is deselected on home from behind search THEN handle mode change`() {
        every { navController.currentDestination } returns mockk {
            every { id } returns R.id.searchDialogFragment
        }

        val url = "https://mozilla.org"
        val tab = createTab(
            id = "otherTab",
            url = url,
            private = true,
            engineSession = mockk(relaxed = true)
        )
        store.dispatch(TabListAction.AddTabAction(tab, select = true)).joinBlocking()

        val newMode = BrowsingMode.Normal
        val hasBeenOnboarded = true

        createController().handlePrivateModeButtonClicked(newMode, hasBeenOnboarded)

        verify(exactly = 0) {
            settings.incrementNumTimesPrivateModeOpened()
        }
        verify {
            AppAction.ModeChange(Mode.fromBrowsingMode(newMode))
            navController.navigate(
                BrowserFragmentDirections.actionGlobalSearchDialog(
                    sessionId = null
                )
            )
        }
    }

    @Test
    fun `WHEN handleReportSessionMetrics is called AND there are zero recent tabs THEN report Event#RecentTabsSectionIsNotVisible`() {
        assertFalse(RecentTabs.sectionVisible.testHasValue())

        every { appState.recentTabs } returns emptyList()
        createController().handleReportSessionMetrics(appState)
        assertTrue(RecentTabs.sectionVisible.testHasValue())
        assertFalse(RecentTabs.sectionVisible.testGetValue())
    }

    @Test
    fun `WHEN handleReportSessionMetrics is called AND there is at least one recent tab THEN report Event#RecentTabsSectionIsVisible`() {
        assertFalse(RecentTabs.sectionVisible.testHasValue())

        val recentTab: RecentTab = mockk(relaxed = true)
        every { appState.recentTabs } returns listOf(recentTab)
        createController().handleReportSessionMetrics(appState)

        assertTrue(RecentTabs.sectionVisible.testHasValue())
        assertTrue(RecentTabs.sectionVisible.testGetValue())
    }

    @Test
    fun `WHEN handleReportSessionMetrics is called AND there are zero recent bookmarks THEN report Event#RecentBookmarkCount(0)`() {
        every { appState.recentBookmarks } returns emptyList()
        every { appState.recentTabs } returns emptyList()
        assertFalse(RecentBookmarks.recentBookmarksCount.testHasValue())

        createController().handleReportSessionMetrics(appState)

        assertTrue(RecentBookmarks.recentBookmarksCount.testHasValue())
        assertEquals(0, RecentBookmarks.recentBookmarksCount.testGetValue())
    }

    @Test
    fun `WHEN handleReportSessionMetrics is called AND there is at least one recent bookmark THEN report Event#RecentBookmarkCount(1)`() {
        val recentBookmark: RecentBookmark = mockk(relaxed = true)
        every { appState.recentBookmarks } returns listOf(recentBookmark)
        every { appState.recentTabs } returns emptyList()
        assertFalse(RecentBookmarks.recentBookmarksCount.testHasValue())

        createController().handleReportSessionMetrics(appState)

        assertTrue(RecentBookmarks.recentBookmarksCount.testHasValue())
        assertEquals(1, RecentBookmarks.recentBookmarksCount.testGetValue())
    }

    @Test
    fun `WHEN handleTopSiteSettingsClicked is called THEN navigate to the HomeSettingsFragment AND report the interaction`() {
        createController().handleTopSiteSettingsClicked()

        assertTrue(TopSites.contileSettings.testHasValue())
        assertEquals(1, TopSites.contileSettings.testGetValue().size)
        assertNull(TopSites.contileSettings.testGetValue().single().extra)
        verify {
            navController.navigate(
                match<NavDirections> {
                    it.actionId == R.id.action_global_homeSettingsFragment
                },
                null
            )
        }
    }

    @Test
    fun `WHEN handleSponsorPrivacyClicked is called THEN navigate to the privacy webpage AND report the interaction`() {
        createController().handleSponsorPrivacyClicked()

        assertTrue(TopSites.contileSponsorsAndPrivacy.testHasValue())
        assertEquals(1, TopSites.contileSponsorsAndPrivacy.testGetValue().size)
        assertNull(TopSites.contileSponsorsAndPrivacy.testGetValue().single().extra)
        verify {
            activity.openToBrowserAndLoad(
                searchTermOrURL = SupportUtils.getGenericSumoURLForTopic(SupportUtils.SumoTopic.SPONSOR_PRIVACY),
                newTab = true,
                from = BrowserDirection.FromHome
            )
        }
    }

    @Test
    fun `WHEN handleOpenInPrivateTabClicked is called with a TopSite#Provided site THEN Event#TopSiteOpenContileInPrivateTab is reported`() {
        val topSite = TopSite.Provided(
            id = 1L,
            title = "Mozilla",
            url = "mozilla.org",
            clickUrl = "",
            imageUrl = "",
            impressionUrl = "",
            createdAt = 0
        )
        createController().handleOpenInPrivateTabClicked(topSite)

        assertTrue(TopSites.openContileInPrivateTab.testHasValue())
        assertEquals(1, TopSites.openContileInPrivateTab.testGetValue().size)
        assertNull(TopSites.openContileInPrivateTab.testGetValue().single().extra)
    }

    @Test
    fun `WHEN handleOpenInPrivateTabClicked is called with a Default, Pinned, or Frecent top site THEN openInPrivateTab event is recorded`() {
        val controller = createController()
        val topSite1 = TopSite.Default(
            id = 1L,
            title = "Mozilla",
            url = "mozilla.org",
            createdAt = 0
        )
        val topSite2 = TopSite.Pinned(
            id = 1L,
            title = "Mozilla",
            url = "mozilla.org",
            createdAt = 0
        )
        val topSite3 = TopSite.Frecent(
            id = 1L,
            title = "Mozilla",
            url = "mozilla.org",
            createdAt = 0
        )
        assertFalse(TopSites.openInPrivateTab.testHasValue())

        controller.handleOpenInPrivateTabClicked(topSite1)
        controller.handleOpenInPrivateTabClicked(topSite2)
        controller.handleOpenInPrivateTabClicked(topSite3)

        assertTrue(TopSites.openInPrivateTab.testHasValue())
        assertEquals(3, TopSites.openInPrivateTab.testGetValue().size)
        for (event in TopSites.openInPrivateTab.testGetValue()) {
            assertNull(event.extra)
        }
    }

    @Test
    fun `WHEN handleMessageClicked,handleMessageClosed and handleMessageDisplayed are called THEN delegate to messageController`() {
        val controller = createController()
        val message = mockk<Message>()

        controller.handleMessageClicked(message)
        controller.handleMessageClosed(message)
        controller.handleMessageDisplayed(message)

        verify {
            messageController.onMessagePressed(message)
        }
        verify {
            messageController.onMessageDismissed(message)
        }
        verify {
            messageController.onMessageDisplayed(message)
        }
    }

    private fun createController(
        hideOnboarding: () -> Unit = { },
        registerCollectionStorageObserver: () -> Unit = { },
        showTabTray: () -> Unit = { },
        removeCollectionWithUndo: (tabCollection: TabCollection) -> Unit = { }
    ): DefaultSessionControlController {
        return DefaultSessionControlController(
            activity = activity,
            settings = settings,
            engine = engine,
            store = store,
            messageController = messageController,
            tabCollectionStorage = tabCollectionStorage,
            addTabUseCase = tabsUseCases.addTab,
            restoreUseCase = mockk(relaxed = true),
            reloadUrlUseCase = reloadUrlUseCase.reload,
            selectTabUseCase = selectTabUseCase.selectTab,
            appStore = appStore,
            navController = navController,
            viewLifecycleScope = scope,
            hideOnboarding = hideOnboarding,
            registerCollectionStorageObserver = registerCollectionStorageObserver,
            removeCollectionWithUndo = removeCollectionWithUndo,
            showTabTray = showTabTray
        )
    }
}
