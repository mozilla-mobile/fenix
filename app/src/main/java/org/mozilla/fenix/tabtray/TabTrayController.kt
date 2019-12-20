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
import org.mozilla.fenix.ext.sessionsOfType

@SuppressWarnings("TooManyFunctions")
interface TabTrayController {
    fun closeTab(tab: Tab)
    fun closeAllTabs()
    fun pauseMedia()
    fun playMedia()
    fun openTab(tab: Tab)
    fun selectTab(tab: Tab)
    fun deselectTab(tab: Tab)
    fun newTab()
    fun enterPrivateBrowsingMode()
    fun exitPrivateBrowsingMode()

    fun navigateToCollectionCreator()
    fun shouldAllowSelect(): Boolean
}

@SuppressWarnings("TooManyFunctions")
class DefaultTabTrayController(
    private val navController: NavController,
    private val sessionManager: SessionManager,
    private val tabTrayFragmentStore: TabTrayFragmentStore,
    private val browsingModeManager: BrowsingModeManager,
    private val tabCloser: (Sequence<Session>, Boolean) -> Unit,
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
        if (navController.currentDestination?.id == R.id.collectionCreationFragment) return
        if (tabTrayFragmentStore.state.mode is TabTrayFragmentState.Mode.Normal) return

        val tabIds = tabTrayFragmentStore.state.mode.selectedTabs.map { it.sessionId }.toTypedArray()
        val directions = TabTrayFragmentDirections.actionTabTrayFragmentToCreateCollectionFragment(
            tabIds = tabIds,
            previousFragmentId = R.id.tabTrayFragment,
            saveCollectionStep = SaveCollectionStep.SelectCollection,
            selectedTabIds = tabIds,
            selectedTabCollectionId = -1
        )

        navController.navigate(directions)
    }

    override fun closeTab(tab: Tab) {
        val sessionToDelete = sessionManager.findSessionById(tab.sessionId) ?: return
        tabCloser(sequenceOf(sessionToDelete), browsingModeManager.mode.isPrivate)
    }

    override fun closeAllTabs() {
        val isPrivate = browsingModeManager.mode.isPrivate
        val tabs = sessionManager.sessionsOfType(isPrivate)
        tabCloser(tabs, isPrivate)
    }

    override fun pauseMedia() { pauseMediaUseCase() }
    override fun playMedia() { playMediaUseCase() }

    override fun openTab(tab: Tab) {
        val session = sessionManager.findSessionById(tab.sessionId) ?: return
        sessionManager.select(session)
        val directions = TabTrayFragmentDirections.actionTabTrayFragmentToBrowserFragment(null)
        navController.navigate(directions)
    }

    override fun selectTab(tab: Tab) {
        tabTrayFragmentStore.dispatch(TabTrayFragmentAction.SelectTab(tab))
    }

    override fun deselectTab(tab: Tab) {
        tabTrayFragmentStore.dispatch(TabTrayFragmentAction.DeselectTab(tab))
    }

    override fun shouldAllowSelect(): Boolean = tabTrayFragmentStore.state.mode.isEditing
}
