/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.appstate

import mozilla.components.feature.tab.collections.TabCollection
import mozilla.components.feature.top.sites.TopSite
import mozilla.components.lib.crash.Crash.NativeCodeCrash
import mozilla.components.lib.state.Action
import mozilla.components.service.pocket.PocketRecommendedStory
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.home.Mode
import org.mozilla.fenix.home.pocket.PocketRecommendedStoriesCategory
import org.mozilla.fenix.home.pocket.PocketRecommendedStoriesSelectedCategory
import org.mozilla.fenix.home.recentbookmarks.RecentBookmark
import org.mozilla.fenix.home.recentsyncedtabs.RecentSyncedTabState
import org.mozilla.fenix.home.recenttabs.RecentTab
import org.mozilla.fenix.home.recentvisits.RecentlyVisitedItem
import org.mozilla.fenix.gleanplumb.Message
import org.mozilla.fenix.gleanplumb.MessagingState

/**
 * [Action] implementation related to [AppStore].
 */
sealed class AppAction : Action {
    data class UpdateInactiveExpanded(val expanded: Boolean) : AppAction()
    data class AddNonFatalCrash(val crash: NativeCodeCrash) : AppAction()
    data class RemoveNonFatalCrash(val crash: NativeCodeCrash) : AppAction()
    object RemoveAllNonFatalCrashes : AppAction()

    data class Change(
        val topSites: List<TopSite>,
        val mode: Mode,
        val collections: List<TabCollection>,
        val showCollectionPlaceholder: Boolean,
        val recentTabs: List<RecentTab>,
        val recentBookmarks: List<RecentBookmark>,
        val recentHistory: List<RecentlyVisitedItem>
    ) :
        AppAction()

    data class CollectionExpanded(val collection: TabCollection, val expand: Boolean) :
        AppAction()

    data class CollectionsChange(val collections: List<TabCollection>) : AppAction()
    data class ModeChange(val mode: Mode) : AppAction()
    data class TopSitesChange(val topSites: List<TopSite>) : AppAction()
    data class RecentTabsChange(val recentTabs: List<RecentTab>) : AppAction()
    data class RemoveRecentTab(val recentTab: RecentTab) : AppAction()
    data class RecentBookmarksChange(val recentBookmarks: List<RecentBookmark>) : AppAction()
    data class RemoveRecentBookmark(val recentBookmark: RecentBookmark) : AppAction()
    data class RecentHistoryChange(val recentHistory: List<RecentlyVisitedItem>) : AppAction()
    data class RemoveRecentHistoryHighlight(val highlightUrl: String) : AppAction()
    data class DisbandSearchGroupAction(val searchTerm: String) : AppAction()
    data class SelectPocketStoriesCategory(val categoryName: String) : AppAction()
    data class DeselectPocketStoriesCategory(val categoryName: String) : AppAction()
    data class PocketStoriesShown(val storiesShown: List<PocketRecommendedStory>) : AppAction()
    data class PocketStoriesChange(val pocketStories: List<PocketRecommendedStory>) : AppAction()
    data class PocketStoriesCategoriesChange(val storiesCategories: List<PocketRecommendedStoriesCategory>) :
        AppAction()
    data class PocketStoriesCategoriesSelectionsChange(
        val storiesCategories: List<PocketRecommendedStoriesCategory>,
        val categoriesSelected: List<PocketRecommendedStoriesSelectedCategory>
    ) : AppAction()
    object RemoveCollectionsPlaceholder : AppAction()

    /**
     * Updates the [RecentSyncedTabState] with the given [state].
     */
    data class RecentSyncedTabStateChange(val state: RecentSyncedTabState) : AppAction()

    /**
     * [Action]s related to interactions with the Messaging Framework.
     */
    sealed class MessagingAction : AppAction() {
        /**
         * Restores the [Message] state from the storage.
         */
        object Restore : MessagingAction()

        /**
         * Evaluates if a new messages should be shown to users.
         */
        object Evaluate : MessagingAction()

        /**
         * Updates [MessagingState.messageToShow] with the given [message].
         */
        data class UpdateMessageToShow(val message: Message) : MessagingAction()

        /**
         * Updates [MessagingState.messageToShow] with the given [message].
         */
        object ConsumeMessageToShow : MessagingAction()

        /**
         * Updates [MessagingState.messages] with the given [messages].
         */
        data class UpdateMessages(val messages: List<Message>) : MessagingAction()

        /**
         * Indicates the given [message] was clicked.
         */
        data class MessageClicked(val message: Message) : MessagingAction()

        /**
         * Indicates the given [message] was shown.
         */
        data class MessageDisplayed(val message: Message) : MessagingAction()

        /**
         * Indicates the given [message] was dismissed.
         */
        data class MessageDismissed(val message: Message) : MessagingAction()
    }
}
