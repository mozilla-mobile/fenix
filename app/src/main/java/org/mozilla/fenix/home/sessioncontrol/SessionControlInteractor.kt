/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol

import mozilla.components.feature.tab.collections.Tab
import mozilla.components.feature.tab.collections.TabCollection
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.service.pocket.PocketRecommendedStory
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.gleanplumb.Message
import org.mozilla.fenix.home.pocket.PocketRecommendedStoriesCategory
import org.mozilla.fenix.home.pocket.PocketStoriesController
import org.mozilla.fenix.home.pocket.PocketStoriesInteractor
import org.mozilla.fenix.home.recentbookmarks.RecentBookmark
import org.mozilla.fenix.home.recentbookmarks.controller.RecentBookmarksController
import org.mozilla.fenix.home.recentbookmarks.interactor.RecentBookmarksInteractor
import org.mozilla.fenix.home.recentsyncedtabs.RecentSyncedTab
import org.mozilla.fenix.home.recenttabs.RecentTab
import org.mozilla.fenix.home.recenttabs.controller.RecentTabController
import org.mozilla.fenix.home.recentsyncedtabs.controller.RecentSyncedTabController
import org.mozilla.fenix.home.recenttabs.interactor.RecentTabInteractor
import org.mozilla.fenix.home.recentsyncedtabs.interactor.RecentSyncedTabInteractor
import org.mozilla.fenix.home.recentvisits.RecentlyVisitedItem.RecentHistoryGroup
import org.mozilla.fenix.home.recentvisits.RecentlyVisitedItem.RecentHistoryHighlight
import org.mozilla.fenix.home.recentvisits.controller.RecentVisitsController
import org.mozilla.fenix.home.recentvisits.interactor.RecentVisitsInteractor

/**
 * Interface for tab related actions in the [SessionControlInteractor].
 */
interface TabSessionInteractor {
    /**
     * Shows the Private Browsing Learn More page in a new tab. Called when a user clicks on the
     * "Common myths about private browsing" link in private mode.
     */
    fun onPrivateBrowsingLearnMoreClicked()

    /**
     * Called when a user clicks on the Private Mode button on the homescreen.
     */
    fun onPrivateModeButtonClicked(newMode: BrowsingMode, userHasBeenOnboarded: Boolean)

    /**
     * Called when there is an update to the session state and updated metrics need to be reported
     *
     * * @param state The state the homepage from which to report desired metrics.
     */
    fun reportSessionMetrics(state: AppState)
}

/**
 * Interface for collection related actions in the [SessionControlInteractor].
 */
@SuppressWarnings("TooManyFunctions")
interface CollectionInteractor {
    /**
     * Shows the Collection Creation fragment for selecting the tabs to add to the given tab
     * collection. Called when a user taps on the "Add tab" collection menu item.
     *
     * @param collection The collection of tabs that will be modified.
     */
    fun onCollectionAddTabTapped(collection: TabCollection)

    /**
     * Opens the given tab. Called when a user clicks on a tab in the tab collection.
     *
     * @param tab The tab to open from the tab collection.
     */
    fun onCollectionOpenTabClicked(tab: Tab)

    /**
     * Opens all the tabs in a given tab collection. Called when a user taps on the "Open tabs"
     * collection menu item.
     *
     * @param collection The collection of tabs to open.
     */
    fun onCollectionOpenTabsTapped(collection: TabCollection)

    /**
     * Removes the given tab from the given tab collection. Called when a user swipes to remove a
     * tab or clicks on the tab close button.
     *
     * @param collection The collection of tabs that will be modified.
     * @param tab The tab to remove from the tab collection.
     */
    fun onCollectionRemoveTab(collection: TabCollection, tab: Tab, wasSwiped: Boolean)

    /**
     * Shares the tabs in the given tab collection. Called when a user clicks on the Collection
     * Share button.
     *
     * @param collection The collection of tabs to share.
     */
    fun onCollectionShareTabsClicked(collection: TabCollection)

    /**
     * Shows a prompt for deleting the given tab collection. Called when a user taps on the
     * "Delete collection" collection menu item.
     *
     * @param collection The collection of tabs to delete.
     */
    fun onDeleteCollectionTapped(collection: TabCollection)

    /**
     * Shows the Collection Creation fragment for renaming the given tab collection. Called when a
     * user taps on the "Rename collection" collection menu item.
     *
     * @param collection The collection of tabs to rename.
     */
    fun onRenameCollectionTapped(collection: TabCollection)

    /**
     * Toggles expanding or collapsing the given tab collection. Called when a user clicks on a
     * [CollectionViewHolder].
     *
     * @param collection The collection of tabs that will be collapsed.
     * @param expand True if the given tab collection should be expanded or collapse if false.
     */
    fun onToggleCollectionExpanded(collection: TabCollection, expand: Boolean)

    /**
     * Opens the collection creator
     */
    fun onAddTabsToCollectionTapped()

    /**
     * User has removed the collections placeholder from home.
     */
    fun onRemoveCollectionsPlaceholder()

    /**
     * User has opened collection 3 dot menu.
     */
    fun onCollectionMenuOpened()
}

interface ToolbarInteractor {
    /**
     * Navigates to browser with clipboard text.
     */
    fun onPasteAndGo(clipboardText: String)

    /**
     * Navigates to search with clipboard text.
     */
    fun onPaste(clipboardText: String)
}

/**
 * Interface for onboarding related actions in the [SessionControlInteractor].
 */
interface OnboardingInteractor {
    /**
     * Hides the onboarding and navigates to Search. Called when a user clicks on the "Start Browsing" button.
     */
    fun onStartBrowsingClicked()

    /**
     * Opens a custom tab to privacy notice url. Called when a user clicks on the "read our privacy notice" button.
     */
    fun onReadPrivacyNoticeClicked()

    /**
     * Show the onboarding dialog to onboard users about recentTabs,recentBookmarks,
     * historyMetadata and pocketArticles sections.
     */
    fun showOnboardingDialog()
}

interface CustomizeHomeIteractor {
    /**
     * Opens the customize home settings page.
     */
    fun openCustomizeHomePage()
}

/**
 * Interface for top site related actions in the [SessionControlInteractor].
 */
interface TopSiteInteractor {
    /**
     * Opens the given top site in private mode. Called when an user clicks on the "Open in private
     * tab" top site menu item.
     *
     * @param topSite The top site that will be open in private mode.
     */
    fun onOpenInPrivateTabClicked(topSite: TopSite)

    /**
     * Opens a dialog to rename the given top site. Called when an user clicks on the "Rename" top site menu item.
     *
     * @param topSite The top site that will be renamed.
     */
    fun onRenameTopSiteClicked(topSite: TopSite)

    /**
     * Removes the given top site. Called when an user clicks on the "Remove" top site menu item.
     *
     * @param topSite The top site that will be removed.
     */
    fun onRemoveTopSiteClicked(topSite: TopSite)

    /**
     * Selects the given top site. Called when a user clicks on a top site.
     *
     * @param topSite The top site that was selected.
     * @param position The position of the top site.
     */
    fun onSelectTopSite(topSite: TopSite, position: Int)

    /**
     * Navigates to the Homepage Settings. Called when an user clicks on the "Settings" top site
     * menu item.
     */
    fun onSettingsClicked()

    /**
     * Opens the sponsor privacy support articles. Called when an user clicks on the
     * "Our sponsors & your privacy" top site menu item.
     */
    fun onSponsorPrivacyClicked()

    /**
     * Called when top site menu is opened.
     */
    fun onTopSiteMenuOpened()
}

interface MessageCardInteractor {
    /**
     * Called when a [Message]'s button is clicked
     */
    fun onMessageClicked(message: Message)

    /**
     * Called when close button on a [Message] card.
     */
    fun onMessageClosedClicked(message: Message)

    /**
     * Called when close button on a [Message] card.
     */
    fun onMessageDisplayed(message: Message)
}

/**
 * Interactor for the Home screen. Provides implementations for the CollectionInteractor,
 * OnboardingInteractor, TopSiteInteractor, TabSessionInteractor, ToolbarInteractor,
 * ExperimentCardInteractor, RecentTabInteractor, RecentBookmarksInteractor
 * and others.
 */
@SuppressWarnings("TooManyFunctions")
class SessionControlInteractor(
    private val controller: SessionControlController,
    private val recentTabController: RecentTabController,
    private val recentSyncedTabController: RecentSyncedTabController,
    private val recentBookmarksController: RecentBookmarksController,
    private val recentVisitsController: RecentVisitsController,
    private val pocketStoriesController: PocketStoriesController
) : CollectionInteractor,
    OnboardingInteractor,
    TopSiteInteractor,
    TabSessionInteractor,
    ToolbarInteractor,
    MessageCardInteractor,
    RecentTabInteractor,
    RecentSyncedTabInteractor,
    RecentBookmarksInteractor,
    RecentVisitsInteractor,
    CustomizeHomeIteractor,
    PocketStoriesInteractor {

    override fun onCollectionAddTabTapped(collection: TabCollection) {
        controller.handleCollectionAddTabTapped(collection)
    }

    override fun onCollectionOpenTabClicked(tab: Tab) {
        controller.handleCollectionOpenTabClicked(tab)
    }

    override fun onCollectionOpenTabsTapped(collection: TabCollection) {
        controller.handleCollectionOpenTabsTapped(collection)
    }

    override fun onCollectionRemoveTab(collection: TabCollection, tab: Tab, wasSwiped: Boolean) {
        controller.handleCollectionRemoveTab(collection, tab, wasSwiped)
    }

    override fun onCollectionShareTabsClicked(collection: TabCollection) {
        controller.handleCollectionShareTabsClicked(collection)
    }

    override fun onDeleteCollectionTapped(collection: TabCollection) {
        controller.handleDeleteCollectionTapped(collection)
    }

    override fun onOpenInPrivateTabClicked(topSite: TopSite) {
        controller.handleOpenInPrivateTabClicked(topSite)
    }

    override fun onRenameTopSiteClicked(topSite: TopSite) {
        controller.handleRenameTopSiteClicked(topSite)
    }

    override fun onRemoveTopSiteClicked(topSite: TopSite) {
        controller.handleRemoveTopSiteClicked(topSite)
    }

    override fun onRenameCollectionTapped(collection: TabCollection) {
        controller.handleRenameCollectionTapped(collection)
    }

    override fun onSelectTopSite(topSite: TopSite, position: Int) {
        controller.handleSelectTopSite(topSite, position)
    }

    override fun onSettingsClicked() {
        controller.handleTopSiteSettingsClicked()
    }

    override fun onSponsorPrivacyClicked() {
        controller.handleSponsorPrivacyClicked()
    }

    override fun onStartBrowsingClicked() {
        controller.handleStartBrowsingClicked()
    }

    override fun onReadPrivacyNoticeClicked() {
        controller.handleReadPrivacyNoticeClicked()
    }

    override fun showOnboardingDialog() {
        controller.handleShowOnboardingDialog()
    }

    override fun onToggleCollectionExpanded(collection: TabCollection, expand: Boolean) {
        controller.handleToggleCollectionExpanded(collection, expand)
    }

    override fun onAddTabsToCollectionTapped() {
        controller.handleCreateCollection()
    }

    override fun onPrivateBrowsingLearnMoreClicked() {
        controller.handlePrivateBrowsingLearnMoreClicked()
    }

    override fun onPrivateModeButtonClicked(newMode: BrowsingMode, userHasBeenOnboarded: Boolean) {
        controller.handlePrivateModeButtonClicked(newMode, userHasBeenOnboarded)
    }

    override fun onPasteAndGo(clipboardText: String) {
        controller.handlePasteAndGo(clipboardText)
    }

    override fun onPaste(clipboardText: String) {
        controller.handlePaste(clipboardText)
    }

    override fun onRemoveCollectionsPlaceholder() {
        controller.handleRemoveCollectionsPlaceholder()
    }

    override fun onCollectionMenuOpened() {
        controller.handleMenuOpened()
    }

    override fun onTopSiteMenuOpened() {
        controller.handleMenuOpened()
    }

    override fun onRecentTabClicked(tabId: String) {
        recentTabController.handleRecentTabClicked(tabId)
    }

    override fun onRecentSearchGroupClicked(tabId: String) {
        recentTabController.handleRecentSearchGroupClicked(tabId)
    }

    override fun onRecentTabShowAllClicked() {
        recentTabController.handleRecentTabShowAllClicked()
    }

    override fun onRemoveRecentTab(tab: RecentTab.Tab) {
        recentTabController.handleRecentTabRemoved(tab)
    }

    override fun onRecentSyncedTabClicked(tab: RecentSyncedTab) {
        recentSyncedTabController.handleRecentSyncedTabClick(tab)
    }

    override fun onSyncedTabShowAllClicked() {
        recentSyncedTabController.handleSyncedTabShowAllClicked()
    }

    override fun onRecentBookmarkClicked(bookmark: RecentBookmark) {
        recentBookmarksController.handleBookmarkClicked(bookmark)
    }

    override fun onShowAllBookmarksClicked() {
        recentBookmarksController.handleShowAllBookmarksClicked()
    }

    override fun onRecentBookmarkRemoved(bookmark: RecentBookmark) {
        recentBookmarksController.handleBookmarkRemoved(bookmark)
    }

    override fun onHistoryShowAllClicked() {
        recentVisitsController.handleHistoryShowAllClicked()
    }

    override fun onRecentHistoryGroupClicked(recentHistoryGroup: RecentHistoryGroup) {
        recentVisitsController.handleRecentHistoryGroupClicked(
            recentHistoryGroup
        )
    }

    override fun onRemoveRecentHistoryGroup(groupTitle: String) {
        recentVisitsController.handleRemoveRecentHistoryGroup(groupTitle)
    }

    override fun onRecentHistoryHighlightClicked(recentHistoryHighlight: RecentHistoryHighlight) {
        recentVisitsController.handleRecentHistoryHighlightClicked(recentHistoryHighlight)
    }

    override fun onRemoveRecentHistoryHighlight(highlightUrl: String) {
        recentVisitsController.handleRemoveRecentHistoryHighlight(highlightUrl)
    }

    override fun openCustomizeHomePage() {
        controller.handleCustomizeHomeTapped()
    }

    override fun onStoriesShown(storiesShown: List<PocketRecommendedStory>) {
        pocketStoriesController.handleStoriesShown(storiesShown)
    }

    override fun onCategoryClicked(categoryClicked: PocketRecommendedStoriesCategory) {
        pocketStoriesController.handleCategoryClick(categoryClicked)
    }

    override fun onStoryClicked(storyClicked: PocketRecommendedStory, storyPosition: Pair<Int, Int>) {
        pocketStoriesController.handleStoryClicked(storyClicked, storyPosition)
    }

    override fun onLearnMoreClicked(link: String) {
        pocketStoriesController.handleLearnMoreClicked(link)
    }

    override fun onDiscoverMoreClicked(link: String) {
        pocketStoriesController.handleDiscoverMoreClicked(link)
    }

    override fun reportSessionMetrics(state: AppState) {
        controller.handleReportSessionMetrics(state)
    }

    override fun onMessageClicked(message: Message) {
        controller.handleMessageClicked(message)
    }

    override fun onMessageClosedClicked(message: Message) {
        controller.handleMessageClosed(message)
    }

    override fun onMessageDisplayed(message: Message) {
        controller.handleMessageDisplayed(message)
    }
}
