/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol

import android.content.Context
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.FragmentNavigator
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.prompt.ShareData
import mozilla.components.feature.media.ext.pauseIfPlaying
import mozilla.components.feature.media.ext.playIfPaused
import mozilla.components.feature.media.state.MediaStateMachine
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager
import org.mozilla.fenix.collections.SaveCollectionStep
import org.mozilla.fenix.components.TabCollectionStorage
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.sessionsOfType
import org.mozilla.fenix.home.HomeFragmentDirections
import org.mozilla.fenix.settings.SupportUtils

/**
 * [HomeFragment] controller. An interface that handles the view manipulation of the Tabs triggered
 * by the Interactor.
 */
interface SessionControlController {
    /**
     * See [TabSessionInteractor.onCloseTab]
     */
    fun handleCloseTab(sessionId: String)

    /**
     * See [TabSessionInteractor.onCloseAllTabs]
     */
    fun handleCloseAllTabs(isPrivateMode: Boolean)

    /**
     * See [TabSessionInteractor.onPauseMediaClicked]
     */
    fun handlePauseMediaClicked()

    /**
     * See [TabSessionInteractor.onPlayMediaClicked]
     */
    fun handlePlayMediaClicked()

    /**
     * See [TabSessionInteractor.onPrivateBrowsingLearnMoreClicked]
     */
    fun handlePrivateBrowsingLearnMoreClicked()

    /**
     * See [TabSessionInteractor.onSaveToCollection]
     */
    fun handleSaveTabToCollection(selectedTabId: String?)

    /**
     * See [TabSessionInteractor.onSelectTab]
     */
    fun handleSelectTab(tabView: View, sessionId: String)

    /**
     * See [TabSessionInteractor.onShareTabs]
     */
    fun handleShareTabs()

    /**
     * See [OnboardingInteractor.onStartBrowsingClicked]
     */
    fun handleStartBrowsingClicked()
}

@SuppressWarnings("TooManyFunctions")
class DefaultSessionControlController(
    private val context: Context,
    private val navController: NavController,
    private val homeLayout: MotionLayout,
    private val browsingModeManager: BrowsingModeManager,
    private val closeTab: (sessionId: String) -> Unit,
    private val closeAllTabs: (isPrivateMode: Boolean) -> Unit,
    private val getListOfTabs: () -> List<Tab>,
    private val hideOnboarding: () -> Unit,
    private val invokePendingDeleteJobs: () -> Unit,
    private val registerCollectionStorageObserver: () -> Unit
) : SessionControlController {
    private val sessionManager: SessionManager
        get() = context.components.core.sessionManager
    private val tabCollectionStorage: TabCollectionStorage
        get() = context.components.core.tabCollectionStorage

    override fun handleCloseTab(sessionId: String) {
        closeTab.invoke(sessionId)
    }

    override fun handleCloseAllTabs(isPrivateMode: Boolean) {
        closeAllTabs.invoke(isPrivateMode)
    }

    override fun handlePauseMediaClicked() {
        MediaStateMachine.state.pauseIfPlaying()
    }

    override fun handlePlayMediaClicked() {
        MediaStateMachine.state.playIfPaused()
    }

    override fun handlePrivateBrowsingLearnMoreClicked() {
        (context as HomeActivity).openToBrowserAndLoad(
            searchTermOrURL = SupportUtils.getGenericSumoURLForTopic
                (SupportUtils.SumoTopic.PRIVATE_BROWSING_MYTHS),
            newTab = true,
            from = BrowserDirection.FromHome
        )
    }

    override fun handleSaveTabToCollection(selectedTabId: String?) {
        if (browsingModeManager.mode.isPrivate) return

        invokePendingDeleteJobs()

        val tabs = getListOfTabs()
        val step = when {
            // Show the SelectTabs fragment if there are multiple opened tabs to select which tabs
            // you want to save to a collection.
            tabs.size > 1 -> SaveCollectionStep.SelectTabs
            // If there is an existing tab collection, show the SelectCollection fragment to save
            // the selected tab to a collection of your choice.
            tabCollectionStorage.cachedTabCollections.isNotEmpty() -> SaveCollectionStep.SelectCollection
            // Show the NameCollection fragment to create a new collection for the selected tab.
            else -> SaveCollectionStep.NameCollection
        }

        showCollectionCreationFragment(step, selectedTabId?.let { arrayOf(it) })
    }

    override fun handleSelectTab(tabView: View, sessionId: String) {
        invokePendingDeleteJobs()
        val session = sessionManager.findSessionById(sessionId)
        sessionManager.select(session!!)
        val directions = HomeFragmentDirections.actionHomeFragmentToBrowserFragment(null)
        val extras =
            FragmentNavigator.Extras.Builder()
                .addSharedElement(tabView,
                    "$TAB_ITEM_TRANSITION_NAME$sessionId"
                )
                .build()
        navController.nav(R.id.homeFragment, directions, extras)
    }

    override fun handleShareTabs() {
        invokePendingDeleteJobs()
        val shareData = sessionManager
            .sessionsOfType(private = browsingModeManager.mode.isPrivate)
            .map { ShareData(url = it.url, title = it.title) }
            .toList()
        showShareFragment(shareData)
    }

    override fun handleStartBrowsingClicked() {
        homeLayout.progress = 0F
        hideOnboarding()
    }

    private fun showCollectionCreationFragment(
        step: SaveCollectionStep,
        selectedTabIds: Array<String>? = null,
        selectedTabCollectionId: Long? = null
    ) {
        if (navController.currentDestination?.id == R.id.collectionCreationFragment) return

        // Only register the observer right before moving to collection creation
        registerCollectionStorageObserver()

        val tabIds = getListOfTabs().map { it.sessionId }.toTypedArray()
        val directions = HomeFragmentDirections.actionHomeFragmentToCreateCollectionFragment(
            tabIds = tabIds,
            previousFragmentId = R.id.homeFragment,
            saveCollectionStep = step,
            selectedTabIds = selectedTabIds,
            selectedTabCollectionId = selectedTabCollectionId ?: -1
        )
        navController.nav(R.id.homeFragment, directions)
    }

    private fun showShareFragment(data: List<ShareData>) {
        val directions = HomeFragmentDirections.actionHomeFragmentToShareFragment(
            data = data.toTypedArray()
        )
        navController.nav(R.id.homeFragment, directions)
    }

    companion object {
        private const val TAB_ITEM_TRANSITION_NAME = "tab_item"
    }
}
