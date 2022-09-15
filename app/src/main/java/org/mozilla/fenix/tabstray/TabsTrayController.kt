/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray

import androidx.annotation.VisibleForTesting
import androidx.navigation.NavController
import mozilla.components.browser.state.action.DebugAction
import mozilla.components.browser.state.action.LastAccessAction
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.selector.getNormalOrPrivateTabs
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.base.profiler.Profiler
import mozilla.components.concept.engine.mediasession.MediaSession.PlaybackState
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.lib.state.DelicateAction
import mozilla.telemetry.glean.private.NoExtras
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.GleanMetrics.Collections
import org.mozilla.fenix.GleanMetrics.Events
import org.mozilla.fenix.GleanMetrics.Tab
import org.mozilla.fenix.GleanMetrics.TabsTray
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.DEFAULT_ACTIVE_DAYS
import org.mozilla.fenix.ext.potentialInactiveTabs
import org.mozilla.fenix.home.HomeFragment
import org.mozilla.fenix.selection.SelectionHolder
import org.mozilla.fenix.tabstray.TrayPagerAdapter.Companion.INACTIVE_TABS_FEATURE_NAME
import org.mozilla.fenix.tabstray.ext.isActiveDownload
import org.mozilla.fenix.tabstray.ext.isSelect
import org.mozilla.fenix.utils.Settings
import java.util.concurrent.TimeUnit
import mozilla.components.browser.storage.sync.Tab as SyncTab

/**
 * Controller contract for handling any and all user interactions in the tabs tray.
 */
interface TabsTrayController {

    /**
     * Called to open a new tab.
     */
    fun handleOpeningNewTab(isPrivate: Boolean)

    /**
     * Called to open a tab.
     */
    fun handleTabOpen(tabId: String, source: String?)

    /**
     * Set the current tray item to the clamped [position].
     *
     * @param tab The [TabSessionState] long pressed by the user.
     * @param holder The [SelectionHolder]<[TabSessionState]> used to manage the selection state.
     */
    fun handleTabLongClick(tab: TabSessionState, holder: SelectionHolder<TabSessionState>): Boolean

    /**
     * Called to select a tab.
     */
    fun handleTabSelected(tab: TabSessionState)

    /**
     * Called to unselect a tab.
     */
    fun handleTabUnselected(tab: TabSessionState)

    /**
     * Handles multi-selection tab clicks.
     */
    fun handleMultiSelectTabClick(
        tab: TabSessionState,
        holder: SelectionHolder<TabSessionState>,
        source: String?,
    )

    /**
     * Set the current tray item to the clamped [position].
     *
     * @param position The position on the tray to focus.
     * @param smoothScroll If true, animate the scrolling from the current tab to [position].
     */
    fun handleTrayScrollingToPosition(position: Int, smoothScroll: Boolean)

    /**
     * Navigate from TabsTray to Browser.
     */
    fun handleNavigateToBrowser()

    /**
     * Deletes the [TabSessionState] with the specified [tabId] or calls [DownloadCancelDialogFragment]
     * if user tries to close the last private tab while private downloads are active.
     * Tracks [Event.ClosedExistingTab] in case of deletion.
     *
     * @param tabId The id of the [TabSessionState] to be removed from TabsTray.
     * @param source app feature from which the tab with [tabId] was closed.
     */
    fun handleTabDeletion(tabId: String, source: String? = null)

    /**
     * Deletes the [TabSessionState] with the specified [tabId]
     * Tracks [Event.ClosedExistingTab] in case of deletion.
     *
     * @param tabId The id of the [TabSessionState] to be removed from TabsTray.
     * @param source app feature from which the tab with [tabId] was closed.
     */
    fun handleDeleteTabWarningAccepted(tabId: String, source: String? = null)

    /**
     * Deletes a list of [tabs].
     *
     * @param tabs List of [TabSessionState]s (sessions) to be removed.
     */
    fun handleMultipleTabsDeletion(tabs: Collection<TabSessionState>)

    /**
     * Moves [tabId] next to before/after [targetId]
     *
     * @param tabId The tabs to be moved
     * @param targetId The id of the tab that the [tab] will be placed next to
     * @param placeAfter Place [tabs] before or after the target
     */
    fun handleTabsMove(tabId: String, targetId: String?, placeAfter: Boolean)

    /**
     * Navigate from TabsTray to Recently Closed section in the History fragment.
     */
    fun handleNavigateToRecentlyClosed()

    /**
     * Set the list of [tabs] into the inactive state.
     *
     * ⚠️ DO NOT USE THIS OUTSIDE OF DEBUGGING/TESTING.
     *
     * @param tabs List of [TabSessionState]s to be removed.
     */
    fun forceTabsAsInactive(
        tabs: Collection<TabSessionState>,
        numOfDays: Long = DEFAULT_ACTIVE_DAYS + 1,
    )

    /**
     * Handles when a tab item is click either to play/pause.
     */
    fun handleMediaClicked(tab: SessionState)

    /**
     * Opens the given inactive tab.
     */
    fun openInactiveTab(tab: TabSessionState)

    /**
     * Closes the given inactive tab.
     */
    fun closeInactiveTab(tab: TabSessionState)

    /**
     * Updates the inactive card to be expanded to display all the tabs, or collapsed with only
     * the title showing.
     */
    fun updateCardExpansion(isExpanded: Boolean)

    /**
     * Dismiss the auto-close dialog.
     */
    fun dismissAutoCloseDialog()

    /**
     * Enable the auto-close feature with the "after a month" setting.
     */
    fun enableInactiveTabsAutoClose()

    /**
     * Delete all inactive tabs.
     */
    fun deleteAllInactiveTabs()

    /**
     * Opens the clicked synced tab.
     */
    fun handleSyncedTabClick(tab: SyncTab)

    /**
     * Consumes the button press and exits multi-selection when in [TabsTrayState.Mode.Select]
     */
    fun handleOnBackPressed(): Boolean
}

@Suppress("TooManyFunctions", "LongParameterList")
class DefaultTabsTrayController(
    private val activity: HomeActivity,
    private val appStore: AppStore,
    private val trayStore: TabsTrayStore,
    private val browserStore: BrowserStore,
    private val browsingModeManager: BrowsingModeManager,
    private val navController: NavController,
    private val navigateToHomeAndDeleteSession: (String) -> Unit,
    private val profiler: Profiler?,
    private val tabsUseCases: TabsUseCases,
    private val selectTabPosition: (Int, Boolean) -> Unit,
    private val dismissTray: () -> Unit,
    private val showUndoSnackbarForTab: (Boolean) -> Unit,
    private val settings: Settings,
    private val selectTab: TabsUseCases.SelectTabUseCase,
    @get:VisibleForTesting
    internal val showCancelledDownloadWarning: (downloadCount: Int, tabId: String?, source: String?) -> Unit,
) : TabsTrayController {

    override fun handleOpeningNewTab(isPrivate: Boolean) {
        val startTime = profiler?.getProfilerTime()
        browsingModeManager.mode = BrowsingMode.fromBoolean(isPrivate)
        navController.navigate(
            TabsTrayFragmentDirections.actionGlobalHome(focusOnAddressBar = true),
        )
        TabsTray.closed.record(NoExtras())
        dismissTray()
        profiler?.addMarker(
            "DefaultTabTrayController.onNewTabTapped",
            startTime,
        )
        sendNewTabEvent(isPrivate)
    }

    override fun handleTabOpen(tabId: String, source: String?) {
        TabsTray.openedExistingTab.record(TabsTray.OpenedExistingTabExtra(source ?: "unknown"))
        selectTab.invoke(tabId)
        handleNavigateToBrowser()
    }

    override fun handleTabLongClick(
        tab: TabSessionState,
        holder: SelectionHolder<TabSessionState>,
    ): Boolean {
        return if (holder.selectedItems.isEmpty()) {
            Collections.longPress.record(NoExtras())
            handleTabSelected(tab)
            true
        } else {
            false
        }
    }

    override fun handleTabSelected(tab: TabSessionState) {
        trayStore.dispatch(TabsTrayAction.AddSelectTab(tab))
    }

    override fun handleTabUnselected(tab: TabSessionState) {
        trayStore.dispatch(TabsTrayAction.RemoveSelectTab(tab))
    }

    override fun handleMultiSelectTabClick(
        tab: TabSessionState,
        holder: SelectionHolder<TabSessionState>,
        source: String?,
    ) {
        val selected = holder.selectedItems
        when {
            selected.isEmpty() && trayStore.state.mode.isSelect().not() -> {
                handleTabOpen(tab.id, source)
            }
            tab.id in selected.map { it.id } -> handleTabUnselected(tab)
            else -> handleTabSelected(tab)
        }
    }

    override fun handleTrayScrollingToPosition(position: Int, smoothScroll: Boolean) {
        selectTabPosition(position, smoothScroll)
        trayStore.dispatch(TabsTrayAction.PageSelected(Page.positionToPage(position)))
    }

    /**
     * Dismisses the tabs tray and navigates to the browser.
     */
    override fun handleNavigateToBrowser() {
        dismissTray()

        if (navController.currentDestination?.id == R.id.browserFragment) {
            return
        } else if (!navController.popBackStack(R.id.browserFragment, false)) {
            navController.navigate(R.id.browserFragment)
        }
    }

    /**
     * Deletes the [TabSessionState] with the specified [tabId].
     *
     * @param tabId The id of the [TabSessionState] to be removed from TabsTray.
     * @param source app feature from which the tab with [tabId] was closed.
     * This method has no effect if the tab does not exist.
     */
    override fun handleTabDeletion(tabId: String, source: String?) {
        deleteTab(tabId, source, isConfirmed = false)
    }

    override fun handleDeleteTabWarningAccepted(tabId: String, source: String?) {
        deleteTab(tabId, source, isConfirmed = true)
    }

    private fun deleteTab(tabId: String, source: String?, isConfirmed: Boolean) {
        val tab = browserStore.state.findTab(tabId)

        tab?.let {
            val isLastTab = browserStore.state.getNormalOrPrivateTabs(it.content.private).size == 1
            if (!isLastTab) {
                tabsUseCases.removeTab(tabId)
                showUndoSnackbarForTab(it.content.private)
            } else {
                val privateDownloads = browserStore.state.downloads.filter { map ->
                    map.value.private && map.value.isActiveDownload()
                }
                if (!isConfirmed && privateDownloads.isNotEmpty()) {
                    showCancelledDownloadWarning(privateDownloads.size, tabId, source)
                    return
                } else {
                    dismissTabsTrayAndNavigateHome(tabId)
                }
            }
            TabsTray.closedExistingTab.record(TabsTray.ClosedExistingTabExtra(source ?: "unknown"))
        }
    }

    /**
     * Deletes a list of [tabs] offering an undo option.
     *
     * @param tabs List of [TabSessionState]s (sessions) to be removed.
     * This method has no effect for tabs that do not exist.
     */
    override fun handleMultipleTabsDeletion(tabs: Collection<TabSessionState>) {
        TabsTray.closeSelectedTabs.record(TabsTray.CloseSelectedTabsExtra(tabCount = tabs.size))

        val isPrivate = tabs.any { it.content.private }

        // If user closes all the tabs from selected tabs page dismiss tray and navigate home.
        if (tabs.size == browserStore.state.getNormalOrPrivateTabs(isPrivate).size) {
            dismissTabsTrayAndNavigateHome(
                if (isPrivate) HomeFragment.ALL_PRIVATE_TABS else HomeFragment.ALL_NORMAL_TABS,
            )
        } else {
            tabs.map { it.id }.let {
                tabsUseCases.removeTabs(it)
            }
        }
        showUndoSnackbarForTab(isPrivate)
    }

    /**
     * Moves [tabId] next to before/after [targetId]
     *
     * @param tabId The tabs to be moved
     * @param targetId The id of the tab that the [tab] will be placed next to
     * @param placeAfter Place [tabs] before or after the target
     */
    override fun handleTabsMove(
        tabId: String,
        targetId: String?,
        placeAfter: Boolean,
    ) {
        if (targetId != null && tabId != targetId) {
            tabsUseCases.moveTabs(listOf(tabId), targetId, placeAfter)
        }
    }

    /**
     * Dismisses the tabs tray and navigates to the Recently Closed section in the History fragment.
     */
    override fun handleNavigateToRecentlyClosed() {
        dismissTray()

        navController.navigate(R.id.recentlyClosedFragment)
    }

    /**
     * Marks all the [tabs] with the [TabSessionState.lastAccess] to 15 days; enough time to
     * have a tab considered as inactive.
     *
     * ⚠️ DO NOT USE THIS OUTSIDE OF DEBUGGING/TESTING.
     */
    @OptIn(DelicateAction::class)
    override fun forceTabsAsInactive(tabs: Collection<TabSessionState>, numOfDays: Long) {
        tabs.forEach { tab ->
            val daysSince = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(numOfDays)
            browserStore.apply {
                dispatch(LastAccessAction.UpdateLastAccessAction(tab.id, daysSince))
                dispatch(DebugAction.UpdateCreatedAtAction(tab.id, daysSince))
            }
        }
    }

    @VisibleForTesting
    internal fun sendNewTabEvent(isPrivateModeSelected: Boolean) {
        if (isPrivateModeSelected) {
            TabsTray.newPrivateTabTapped.record(NoExtras())
        } else {
            TabsTray.newTabTapped.record(NoExtras())
        }
    }

    @VisibleForTesting
    internal fun dismissTabsTrayAndNavigateHome(sessionId: String) {
        dismissTray()
        navigateToHomeAndDeleteSession(sessionId)
    }

    override fun handleMediaClicked(tab: SessionState) {
        when (tab.mediaSessionState?.playbackState) {
            PlaybackState.PLAYING -> {
                Tab.mediaPause.record(NoExtras())
                tab.mediaSessionState?.controller?.pause()
            }

            PlaybackState.PAUSED -> {
                Tab.mediaPlay.record(NoExtras())
                tab.mediaSessionState?.controller?.play()
            }
            else -> throw AssertionError(
                "Play/Pause button clicked without play/pause state.",
            )
        }
    }

    override fun openInactiveTab(tab: TabSessionState) {
        TabsTray.openInactiveTab.add()
        handleNavigateToBrowser()
    }

    override fun closeInactiveTab(tab: TabSessionState) {
        TabsTray.closeInactiveTab.add()
        handleTabDeletion(tab.id, INACTIVE_TABS_FEATURE_NAME)
    }

    override fun updateCardExpansion(isExpanded: Boolean) {
        appStore.dispatch(AppAction.UpdateInactiveExpanded(isExpanded))

        when (isExpanded) {
            true -> TabsTray.inactiveTabsExpanded.record(NoExtras())
            false -> TabsTray.inactiveTabsCollapsed.record(NoExtras())
        }
    }

    override fun dismissAutoCloseDialog() {
        markDialogAsShown()
        TabsTray.autoCloseDimissed.record(NoExtras())
    }

    override fun enableInactiveTabsAutoClose() {
        markDialogAsShown()
        settings.closeTabsAfterOneMonth = true
        settings.closeTabsAfterOneWeek = false
        settings.closeTabsAfterOneDay = false
        settings.manuallyCloseTabs = false
        TabsTray.autoCloseTurnOnClicked.record(NoExtras())
    }

    override fun deleteAllInactiveTabs() {
        TabsTray.closeAllInactiveTabs.record(NoExtras())
        browserStore.state.potentialInactiveTabs.map { it.id }.let {
            tabsUseCases.removeTabs(it)
        }
        showUndoSnackbarForTab(false)
    }

    override fun handleSyncedTabClick(tab: SyncTab) {
        Events.syncedTabOpened.record(NoExtras())

        dismissTray()
        activity.openToBrowserAndLoad(
            searchTermOrURL = tab.active().url,
            newTab = true,
            from = BrowserDirection.FromTabsTray,
        )
    }

    override fun handleOnBackPressed(): Boolean {
        if (trayStore.state.mode is TabsTrayState.Mode.Select) {
            trayStore.dispatch(TabsTrayAction.ExitSelectMode)
            return true
        }
        return false
    }

    /**
     * Marks the dialog as shown and to not be displayed again.
     */
    private fun markDialogAsShown() {
        settings.hasInactiveTabsAutoCloseDialogBeenDismissed = true
    }
}
