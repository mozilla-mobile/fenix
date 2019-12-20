/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home

import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.navigation.NavController
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import mozilla.components.feature.tab.collections.TabCollection
import org.junit.After
import mozilla.components.feature.tab.collections.Tab as ComponentTab
import org.junit.Before
import org.junit.Test
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.home.sessioncontrol.DefaultSessionControlController
import org.mozilla.fenix.settings.SupportUtils

@ExperimentalCoroutinesApi
@UseExperimental(ObsoleteCoroutinesApi::class)
class DefaultSessionControlControllerTest {

    private val mainThreadSurrogate = newSingleThreadContext("UI thread")
    private val activity: HomeActivity = mockk(relaxed = true)
    private val store: HomeFragmentStore = mockk(relaxed = true)
    private val navController: NavController = mockk(relaxed = true)
    private val homeLayout: MotionLayout = mockk(relaxed = true)
    private val browsingModeManager: BrowsingModeManager = mockk(relaxed = true)
    private val getListOfTabs: () -> List<Tab> = { emptyList() }
    private val hideOnboarding: () -> Unit = mockk(relaxed = true)
    private val registerCollectionStorageObserver: () -> Unit = mockk(relaxed = true)
    private val scrollToTheTop: () -> Unit = mockk(relaxed = true)
    private val showDeleteCollectionPrompt: (tabCollection: TabCollection) -> Unit =
        mockk(relaxed = true)
    private val metrics: MetricController = mockk(relaxed = true)
    private val state: HomeFragmentState = mockk(relaxed = true)

    private lateinit var controller: DefaultSessionControlController

    @Before
    fun setup() {
        Dispatchers.setMain(mainThreadSurrogate)

        every { store.state } returns state
        every { state.collections } returns emptyList()
        every { state.expandedCollections } returns emptySet()
        every { state.mode } returns Mode.Normal
        every { state.tabs } returns emptyList()
        every { activity.components.analytics.metrics } returns metrics

        controller = DefaultSessionControlController(
            activity = activity,
            store = store,
            navController = navController,
            homeLayout = homeLayout,
            browsingModeManager = browsingModeManager,
            lifecycleScope = MainScope(),
            getListOfTabs = getListOfTabs,
            hideOnboarding = hideOnboarding,
            registerCollectionStorageObserver = registerCollectionStorageObserver,
            scrollToTheTop = scrollToTheTop,
            showDeleteCollectionPrompt = showDeleteCollectionPrompt
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    @Test
    fun handleCollectionAddTabTapped() {
        val collection: TabCollection = mockk(relaxed = true)
        controller.handleCollectionAddTabTapped(collection)
        verify { metrics.track(Event.CollectionAddTabPressed) }
    }

    @Test
    fun handleCollectionOpenTabClicked() {
        val tab: ComponentTab = mockk(relaxed = true)
        controller.handleCollectionOpenTabClicked(tab)
        verify { metrics.track(Event.CollectionTabRestored) }
    }

    @Test
    fun handleCollectionOpenTabsTapped() {
        val collection: TabCollection = mockk(relaxed = true)
        controller.handleCollectionOpenTabsTapped(collection)
        verify { scrollToTheTop() }
        verify { metrics.track(Event.CollectionAllTabsRestored) }
    }

    @Test
    fun handleCollectionRemoveTab() {
        val collection: TabCollection = mockk(relaxed = true)
        val tab: ComponentTab = mockk(relaxed = true)
        controller.handleCollectionRemoveTab(collection, tab)
        verify { metrics.track(Event.CollectionTabRemoved) }
    }

    @Test
    fun handleCollectionShareTabsClicked() {
        val collection: TabCollection = mockk(relaxed = true)
        controller.handleCollectionShareTabsClicked(collection)
        verify { metrics.track(Event.CollectionShared) }
    }

    @Test
    fun handleDeleteCollectionTapped() {
        val collection: TabCollection = mockk(relaxed = true)
        controller.handleDeleteCollectionTapped(collection)
        verify { showDeleteCollectionPrompt(collection) }
    }

    @Test
    fun handlePrivateBrowsingLearnMoreClicked() {
        controller.handlePrivateBrowsingLearnMoreClicked()
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
        val collection: TabCollection = mockk(relaxed = true)
        controller.handleRenameCollectionTapped(collection)
        verify { metrics.track(Event.CollectionRenamePressed) }
    }

    @Test
    fun handleStartBrowsingClicked() {
        controller.handleStartBrowsingClicked()
        verify { hideOnboarding() }
    }

    @Test
    fun handleToggleCollectionExpanded() {
        val collection: TabCollection = mockk(relaxed = true)
        controller.handleToggleCollectionExpanded(collection, true)
        verify { store.dispatch(HomeFragmentAction.CollectionExpanded(collection, true)) }
    }
}
