/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser

import android.content.Context
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.navigation.NavController
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.action.RestoreCompleteAction
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.LoadRequestState
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.support.test.ext.joinBlocking
import mozilla.components.support.test.rule.MainCoroutineRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.FenixApplication
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.components.toolbar.BrowserToolbarView
import org.mozilla.fenix.ext.application
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.onboarding.FenixOnboarding

@ExperimentalCoroutinesApi
@RunWith(FenixRobolectricTestRunner::class)
class BrowserFragmentTest {

    private lateinit var store: BrowserStore
    private lateinit var testTab: TabSessionState
    private lateinit var browserFragment: BrowserFragment
    private lateinit var view: View
    private lateinit var homeActivity: HomeActivity
    private lateinit var fenixApplication: FenixApplication
    private lateinit var context: Context
    private lateinit var lifecycleOwner: MockedLifecycleOwner
    private lateinit var navController: NavController
    private lateinit var onboarding: FenixOnboarding

    private val testDispatcher = TestCoroutineDispatcher()

    @get:Rule
    val coroutinesTestRule = MainCoroutineRule(testDispatcher)

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        fenixApplication = mockk(relaxed = true)
        every { context.application } returns fenixApplication

        homeActivity = mockk(relaxed = true)
        view = mockk(relaxed = true)
        lifecycleOwner = MockedLifecycleOwner(Lifecycle.State.STARTED)
        navController = mockk(relaxed = true)
        onboarding = mockk(relaxed = true)

        browserFragment = spyk(BrowserFragment())
        every { browserFragment.view } returns view
        every { browserFragment.isAdded } returns true
        every { browserFragment.browserToolbarView } returns mockk(relaxed = true)
        every { browserFragment.activity } returns homeActivity
        every { browserFragment.lifecycle } returns lifecycleOwner.lifecycle
        every { browserFragment.onboarding } returns onboarding

        every { browserFragment.requireContext() } returns context
        every { browserFragment.initializeUI(any()) } returns mockk()
        every { browserFragment.fullScreenChanged(any()) } returns Unit
        every { browserFragment.resumeDownloadDialogState(any(), any(), any(), any(), any()) } returns Unit

        store = BrowserStore()
        every { context.components.core.store } returns store
        testTab = createTab(url = "https://mozilla.org")
    }

    @After
    fun cleanUp() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `GIVEN fragment is added WHEN selected tab changes THEN theme is updated`() {
        browserFragment.observeTabSelection(store)
        verify(exactly = 0) { browserFragment.updateThemeForSession(testTab) }

        addAndSelectTab(testTab)
        verify(exactly = 1) { browserFragment.updateThemeForSession(testTab) }
    }

    @Test
    fun `GIVEN fragment is removing WHEN selected tab changes THEN theme is not updated`() {
        every { browserFragment.isRemoving } returns true
        browserFragment.observeTabSelection(store)

        addAndSelectTab(testTab)
        verify(exactly = 0) { browserFragment.updateThemeForSession(testTab) }
    }

    @Test
    fun `GIVEN browser UI is not initialized WHEN selected tab changes THEN browser UI is initialized`() {
        browserFragment.observeTabSelection(store)
        verify(exactly = 0) { browserFragment.initializeUI(view) }

        addAndSelectTab(testTab)
        verify(exactly = 1) { browserFragment.initializeUI(view) }
    }

    @Test
    fun `GIVEN browser UI is initialized WHEN selected tab changes THEN toolbar is expanded`() {
        browserFragment.browserInitialized = true
        browserFragment.observeTabSelection(store)

        val toolbar: BrowserToolbarView = mockk(relaxed = true)
        every { browserFragment.browserToolbarView } returns toolbar

        val newSelectedTab = createTab("https://firefox.com")
        addAndSelectTab(newSelectedTab)
        verify(exactly = 1) { toolbar.expand() }
    }

    @Test
    fun `GIVEN browser UI is initialized WHEN selected tab changes THEN full screen mode is exited`() {
        browserFragment.browserInitialized = true
        browserFragment.observeTabSelection(store)

        val newSelectedTab = createTab("https://firefox.com")
        addAndSelectTab(newSelectedTab)
        verify(exactly = 1) { browserFragment.fullScreenChanged(false) }
    }

    @Test
    fun `GIVEN browser UI is initialized WHEN selected tab changes THEN download dialog is resumed`() {
        browserFragment.browserInitialized = true
        browserFragment.observeTabSelection(store)

        val newSelectedTab = createTab("https://firefox.com")
        addAndSelectTab(newSelectedTab)
        verify(exactly = 1) {
            browserFragment.resumeDownloadDialogState(newSelectedTab.id, store, view, context, any())
        }
    }

    @Test
    fun `WHEN url changes THEN toolbar is expanded`() {
        addAndSelectTab(testTab)
        browserFragment.expandToolbarOnNavigation(store)

        val toolbar: BrowserToolbarView = mockk(relaxed = true)
        every { browserFragment.browserToolbarView } returns toolbar

        store.dispatch(ContentAction.UpdateUrlAction(testTab.id, "https://firefox.com")).joinBlocking()
        verify(exactly = 1) { toolbar.expand() }
    }

    @Test
    fun `WHEN load request is triggered THEN toolbar is expanded`() {
        addAndSelectTab(testTab)
        browserFragment.expandToolbarOnNavigation(store)

        val toolbar: BrowserToolbarView = mockk(relaxed = true)
        every { browserFragment.browserToolbarView } returns toolbar

        store.dispatch(ContentAction.UpdateLoadRequestAction(
            testTab.id,
            LoadRequestState("https://firefox.com", false, true))
        ).joinBlocking()
        verify(exactly = 1) { toolbar.expand() }
    }

    @Test
    fun `GIVEN tabs are restored WHEN there are no tabs THEN navigate to home`() {
        browserFragment.observeRestoreComplete(store, navController)
        store.dispatch(RestoreCompleteAction).joinBlocking()

        verify(exactly = 1) { navController.popBackStack(R.id.homeFragment, false) }
    }

    @Test
    fun `GIVEN tabs are restored WHEN there are tabs THEN do not navigate`() {
        addAndSelectTab(testTab)
        browserFragment.observeRestoreComplete(store, navController)
        store.dispatch(RestoreCompleteAction).joinBlocking()

        verify(exactly = 0) { navController.popBackStack(R.id.homeFragment, false) }
    }

    @Test
    fun `GIVEN tabs are restored WHEN there is no selected tab THEN navigate to home`() {
        val store = BrowserStore(initialState = BrowserState(tabs = listOf(testTab)))
        browserFragment.observeRestoreComplete(store, navController)
        store.dispatch(RestoreCompleteAction).joinBlocking()

        verify(exactly = 1) { navController.popBackStack(R.id.homeFragment, false) }
    }

    @Test
    fun `GIVEN the onboarding is finished WHEN visiting any link THEN the onboarding is not dismissed `() {
        every { onboarding.userHasBeenOnboarded() } returns true

        browserFragment.observeTabSource(store)

        val newSelectedTab = createTab("any-tab.org")
        addAndSelectTab(newSelectedTab)

        verify(exactly = 0) { onboarding.finish() }
    }

    @Test
    fun `GIVEN the onboarding is not finished WHEN visiting a link THEN the onboarding is dismissed `() {
        every { onboarding.userHasBeenOnboarded() } returns false

        browserFragment.observeTabSource(store)

        val newSelectedTab = createTab("any-tab.org")
        addAndSelectTab(newSelectedTab)

        verify(exactly = 1) { onboarding.finish() }
    }

    @Test
    fun `GIVEN the onboarding is not finished WHEN visiting an onboarding link THEN the onboarding is not dismissed `() {
        every { onboarding.userHasBeenOnboarded() } returns false

        browserFragment.observeTabSource(store)

        val newSelectedTab = createTab(BaseBrowserFragment.onboardingLinksList[0])
        addAndSelectTab(newSelectedTab)

        verify(exactly = 0) { onboarding.finish() }
    }

    @Test
    fun `GIVEN the onboarding is not finished WHEN opening a page from another app THEN the onboarding is not dismissed `() {
        every { onboarding.userHasBeenOnboarded() } returns false

        browserFragment.observeTabSource(store)

        val newSelectedTab1: TabSessionState = mockk(relaxed = true)
        val newSelectedTab2: TabSessionState = mockk(relaxed = true)
        val newSelectedTab3: TabSessionState = mockk(relaxed = true)

        every { newSelectedTab1.source } returns SessionState.Source.ACTION_SEARCH
        every { newSelectedTab2.source } returns SessionState.Source.ACTION_SEND
        every { newSelectedTab3.source } returns SessionState.Source.ACTION_VIEW

        addAndSelectTab(newSelectedTab1)
        verify(exactly = 0) { onboarding.finish() }

        addAndSelectTab(newSelectedTab2)
        verify(exactly = 0) { onboarding.finish() }

        addAndSelectTab(newSelectedTab3)
        verify(exactly = 0) { onboarding.finish() }
    }

    @Test
    fun `GIVEN the onboarding is not finished WHEN visiting an link after redirect THEN the onboarding is not dismissed `() {
        every { onboarding.userHasBeenOnboarded() } returns false

        val newSelectedTab: TabSessionState = mockk(relaxed = true)
        every { newSelectedTab.content.loadRequest?.triggeredByRedirect } returns true

        browserFragment.observeTabSource(store)
        addAndSelectTab(newSelectedTab)

        verify(exactly = 0) { onboarding.finish() }
    }

    @Test
    fun `WHEN isPullToRefreshEnabledInBrowser is disabled THEN pull down refresh is disabled`() {
        every { homeActivity.isImmersive } returns false
        every { context.settings().isPullToRefreshEnabledInBrowser } returns true
        assert(browserFragment.shouldPullToRefreshBeEnabled())

        every { context.settings().isPullToRefreshEnabledInBrowser } returns false
        assert(!browserFragment.shouldPullToRefreshBeEnabled())
    }

    @Test
    fun `WHEN in immersive mode THEN pull down refresh is disabled`() {
        every { homeActivity.isImmersive } returns false
        every { context.settings().isPullToRefreshEnabledInBrowser } returns true
        assert(browserFragment.shouldPullToRefreshBeEnabled())

        every { homeActivity.isImmersive } returns true
        assert(!browserFragment.shouldPullToRefreshBeEnabled())
    }

    private fun addAndSelectTab(tab: TabSessionState) {
        store.dispatch(TabListAction.AddTabAction(tab)).joinBlocking()
        store.dispatch(TabListAction.SelectTabAction(tab.id)).joinBlocking()
    }

    internal class MockedLifecycleOwner(initialState: Lifecycle.State) : LifecycleOwner {
        val lifecycleRegistry = LifecycleRegistry(this).apply {
            currentState = initialState
        }

        override fun getLifecycle(): Lifecycle = lifecycleRegistry
    }
}
