/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabtray

import androidx.navigation.NavController
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.feature.media.ext.pauseIfPlaying
import mozilla.components.feature.media.ext.playIfPaused
import mozilla.components.feature.media.state.MediaStateMachine
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager
import org.mozilla.fenix.collections.SaveCollectionStep
import org.mozilla.fenix.components.TabCollectionStorage
import org.mozilla.fenix.ext.sessionsOfType

@SuppressWarnings("TooManyFunctions")
interface TabTrayController {
    fun closeTab(tab: Tab)
    fun closeAllTabs()
    fun pauseMedia()
    fun playMedia()
    fun openTab(tab: Tab)
    fun newTab()
    fun enterPrivateBrowsingMode()
    fun exitPrivateBrowsingMode()
    fun goHome()

    fun navigateToCollectionCreator()
}

@SuppressWarnings("TooManyFunctions")
class DefaultTabTrayController(
    private val tabCollectionStorage: TabCollectionStorage,
    private val navController: NavController,
    private val sessionManager: SessionManager,
    private val tabTrayFragmentStore: TabTrayFragmentStore,
    private val browsingModeManager: BrowsingModeManager,
    private val closeTabAction: (Sequence<Session>, Boolean) -> Unit,
    private val onModeChange: (BrowsingMode) -> Unit,
    private val pauseMediaUseCase: () -> Unit = { MediaStateMachine.state.pauseIfPlaying() },
    private val playMediaUseCase: () -> Unit = { MediaStateMachine.state.playIfPaused() }
) : TabTrayController {
    override fun enterPrivateBrowsingMode() {
        val newMode = BrowsingMode.Private

        browsingModeManager.mode = newMode
        onModeChange(newMode)
    }

    override fun exitPrivateBrowsingMode() {
        val newMode = BrowsingMode.Normal

        browsingModeManager.mode = newMode
        onModeChange(newMode)
    }

    override fun newTab() {
        val directions = TabTrayFragmentDirections.actionTabTrayFragmentToSearchFragment(null)
        navController.navigate(directions)
    }

    override fun navigateToCollectionCreator() {
        val tabs = sessionManager.sessionsOfType(false)
        val tabIds = tabs.map { it.id }.toList().toTypedArray()

        val step = when {
            // Show the SelectTabs fragment if there are multiple opened tabs to select which tabs
            // you want to save to a collection.
            tabs.count() > 1 -> SaveCollectionStep.SelectTabs
            // If there is an existing tab collection, show the SelectCollection fragment to save
            // the selected tab to a collection of your choice.
            tabCollectionStorage.cachedTabCollections.isNotEmpty() -> SaveCollectionStep.SelectCollection
            // Show the NameCollection fragment to create a new collection for the selected tab.
            else -> SaveCollectionStep.NameCollection
        }
        val directions = TabTrayFragmentDirections.actionTabTrayFragmentToCreateCollectionFragment(
            tabIds = tabIds,
            previousFragmentId = R.id.tabTrayFragment,
            saveCollectionStep = step,
            selectedTabIds = tabIds,
            selectedTabCollectionId = -1
        )

        navController.navigate(directions)
    }

    override fun closeTab(tab: Tab) {
        val sessionToDelete = sessionManager.findSessionById(tab.sessionId) ?: return
        closeTabAction(sequenceOf(sessionToDelete), browsingModeManager.mode.isPrivate)
    }

    override fun closeAllTabs() {
        val isPrivate = browsingModeManager.mode.isPrivate
        val tabs = sessionManager.sessionsOfType(isPrivate)
        closeTabAction(tabs, isPrivate)
    }

    override fun goHome() {
        navController.navigate(TabTrayFragmentDirections.actionTabTrayFragmentToHomeFragment())
    }

    override fun pauseMedia() { pauseMediaUseCase() }
    override fun playMedia() { playMediaUseCase() }

    override fun openTab(tab: Tab) {
        val session = sessionManager.findSessionById(tab.sessionId) ?: return
        sessionManager.select(session)
        val directions = TabTrayFragmentDirections.actionTabTrayFragmentToBrowserFragment(null)
        navController.navigate(directions)
    }
}
