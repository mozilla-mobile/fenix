/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray

import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.tabstray.TabsTray
import mozilla.components.support.base.feature.UserInteractionHandler
import org.mozilla.fenix.selection.SelectionHolder
import org.mozilla.fenix.selection.SelectionInteractor
import org.mozilla.fenix.tabstray.browser.InactiveTabsInteractor
import mozilla.components.browser.storage.sync.Tab as SyncTab

/**
 * Interactor contract for responding to any and all user interactions in the tabs tray.
 */
interface TabsTrayInteractor :
    InactiveTabsInteractor,
    SelectionInteractor<TabSessionState>,
    UserInteractionHandler,
    TabsTray.Delegate {

    /**
     * Set the current tray item to the clamped [position].
     *
     * @param position The position on the tray to focus.
     * @param smoothScroll If true, animate the scrolling from the current tab to [position].
     */
    fun onTrayPositionSelected(position: Int, smoothScroll: Boolean)

    /**
     * Dismisses the tabs tray and navigates to the browser.
     */
    fun onBrowserTabSelected()

    /**
     * Invoked when the user confirmed tab removal that would lead to cancelled private downloads.
     * @param source is the app feature from which the [TabSessionState] with [tabId] was closed.
     */
    fun onDeletePrivateTabWarningAccepted(tabId: String, source: String? = null)

    /**
     * Invoked when [TabSessionState]s need to be deleted.
     */
    fun onDeleteTabs(tabs: Collection<TabSessionState>)

    /**
     * Called when clicking the debug menu option for inactive tabs.
     */
    fun onInactiveDebugClicked(tabs: Collection<TabSessionState>)

    /**
     * Invoked when [tabId] should be moved to before/after [targetId] from a drag-drop operation
     */
    fun onTabsMove(
        tabId: String,
        targetId: String?,
        placeAfter: Boolean,
    )

    /**
     * Invoked when the user clicks on the TabTray's Floating Action Button.
     */
    fun onFabClicked(isPrivate: Boolean)

    /**
     * Invoked when the Recently Closed item is clicked.
     */
    fun onRecentlyClosedClicked()

    /**
     * Called when clicking on a [SyncTab] item.
     */
    fun onSyncedTabClicked(tab: SyncTab)

    /**
     * Indicates Play/Pause item is clicked.
     *
     * @param tab [TabSessionState] to close.
     */
    fun onMediaClicked(tab: TabSessionState)

    /**
     * Handles clicks when multi-selection is enabled.
     */
    fun onMultiSelectClicked(
        tab: TabSessionState,
        holder: SelectionHolder<TabSessionState>,
        source: String?,
    )

    /**
     * Handles long click events when a tab is clicked.
     */
    fun onTabLongClicked(
        tab: TabSessionState,
        holder: SelectionHolder<TabSessionState>,
    ): Boolean
}

/**
 * Interactor to be called for any tabs tray user actions.
 *
 * @property controller [TabsTrayController] to which user actions can be delegated for actual app update.
 */
@Suppress("TooManyFunctions")
class DefaultTabsTrayInteractor(
    private val controller: TabsTrayController,
) : TabsTrayInteractor {

    override fun onTrayPositionSelected(position: Int, smoothScroll: Boolean) {
        controller.handleTrayScrollingToPosition(position, smoothScroll)
    }

    override fun onBrowserTabSelected() {
        controller.handleNavigateToBrowser()
    }

    override fun onDeletePrivateTabWarningAccepted(tabId: String, source: String?) {
        controller.handleDeleteTabWarningAccepted(tabId, source)
    }

    override fun onDeleteTabs(tabs: Collection<TabSessionState>) {
        controller.handleMultipleTabsDeletion(tabs)
    }

    override fun onTabsMove(
        tabId: String,
        targetId: String?,
        placeAfter: Boolean,
    ) {
        controller.handleTabsMove(tabId, targetId, placeAfter)
    }

    override fun onInactiveDebugClicked(tabs: Collection<TabSessionState>) {
        controller.forceTabsAsInactive(tabs)
    }

    override fun onFabClicked(isPrivate: Boolean) {
        controller.handleOpeningNewTab(isPrivate)
    }

    override fun onRecentlyClosedClicked() {
        controller.handleNavigateToRecentlyClosed()
    }

    override fun onMediaClicked(tab: TabSessionState) {
        controller.handleMediaClicked(tab)
    }

    override fun onMultiSelectClicked(
        tab: TabSessionState,
        holder: SelectionHolder<TabSessionState>,
        source: String?,
    ) {
        controller.handleMultiSelectTabClick(
            tab = tab,
            holder = holder,
            source = source,
        )
    }

    override fun onTabLongClicked(
        tab: TabSessionState,
        holder: SelectionHolder<TabSessionState>,
    ): Boolean {
        return controller.handleTabLongClick(tab, holder)
    }

    override fun onSyncedTabClicked(tab: SyncTab) {
        controller.handleSyncedTabClick(tab)
    }

    override fun onHeaderClicked(activated: Boolean) {
        controller.updateCardExpansion(activated)
    }

    override fun onCloseClicked() {
        controller.dismissAutoCloseDialog()
    }

    override fun onEnableAutoCloseClicked() {
        controller.enableInactiveTabsAutoClose()
    }

    override fun onInactiveTabClicked(tab: TabSessionState) {
        controller.openInactiveTab(tab)
    }

    override fun onInactiveTabClosed(tab: TabSessionState) {
        controller.closeInactiveTab(tab)
    }

    override fun onDeleteAllInactiveTabsClicked() {
        controller.deleteAllInactiveTabs()
    }

    override fun open(item: TabSessionState) {
        openTab(item.id, null)
    }

    override fun select(item: TabSessionState) {
        controller.handleTabSelected(item)
    }

    override fun deselect(item: TabSessionState) {
        controller.handleTabUnselected(item)
    }

    override fun onBackPressed(): Boolean {
        return controller.handleOnBackPressed()
    }

    override fun onTabClosed(tab: TabSessionState, source: String?) {
        closeTab(tab.id, source)
    }

    override fun onTabSelected(tab: TabSessionState, source: String?) {
        openTab(tab.id, source)
    }

    private fun openTab(tabId: String, source: String? = null) {
        controller.handleTabOpen(tabId, source)
    }

    private fun closeTab(tabId: String, source: String? = null) {
        controller.handleTabDeletion(tabId, source)
    }
}
