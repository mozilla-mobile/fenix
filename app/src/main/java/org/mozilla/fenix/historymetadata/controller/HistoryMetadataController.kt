/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.historymetadata.controller

import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mozilla.components.browser.state.action.HistoryMetadataAction
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.storage.HistoryMetadataStorage
import mozilla.components.feature.tabs.TabsUseCases.SelectOrAddUseCase
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.historymetadata.RecentlyVisitedItem.RecentHistoryGroup
import org.mozilla.fenix.historymetadata.RecentlyVisitedItem.RecentHistoryHighlight
import org.mozilla.fenix.historymetadata.interactor.HistoryMetadataInteractor
import org.mozilla.fenix.home.HomeFragmentAction
import org.mozilla.fenix.home.HomeFragmentAction.RemoveRecentHistoryHighlight
import org.mozilla.fenix.home.HomeFragmentDirections
import org.mozilla.fenix.home.HomeFragmentStore
import org.mozilla.fenix.library.history.toHistoryMetadata

/**
 * An interface that handles the view manipulation of the history metadata in the Home screen.
 */
interface HistoryMetadataController {

    /**
     * @see [HistoryMetadataInteractor.onHistoryShowAllClicked]
     */
    fun handleHistoryShowAllClicked()

    /**
     * @see [HistoryMetadataInteractor.onRecentHistoryGroupClicked]
     */
    fun handleRecentHistoryGroupClicked(recentHistoryGroup: RecentHistoryGroup)

    /**
     * @see [HistoryMetadataInteractor.onRemoveRecentHistoryGroup]
     */
    fun handleRemoveRecentHistoryGroup(searchTerm: String)

    /**
     * @see [HistoryMetadataInteractor.onRecentHistoryHighlightClicked]
     */
    fun handleRecentHistoryHighlightClicked(recentHistoryHighlight: RecentHistoryHighlight)

    /**
     * @see [HistoryMetadataInteractor.onRemoveRecentHistoryHighlight]
     */
    fun handleRemoveRecentHistoryHighlight(url: String)
}

/**
 * The default implementation of [HistoryMetadataController].
 */
class DefaultHistoryMetadataController(
    private val store: BrowserStore,
    private val homeStore: HomeFragmentStore,
    private val selectOrAddTabUseCase: SelectOrAddUseCase,
    private val navController: NavController,
    private val storage: HistoryMetadataStorage,
    private val scope: CoroutineScope,
    private val metrics: MetricController
) : HistoryMetadataController {

    override fun handleHistoryShowAllClicked() {
        dismissSearchDialogIfDisplayed()
        navController.navigate(
            HomeFragmentDirections.actionGlobalHistoryFragment()
        )
    }

    override fun handleRecentHistoryGroupClicked(recentHistoryGroup: RecentHistoryGroup) {
        navController.navigate(
            HomeFragmentDirections.actionGlobalHistoryMetadataGroup(
                title = recentHistoryGroup.title,
                historyMetadataItems = recentHistoryGroup.historyMetadata
                    .map { it.toHistoryMetadata() }.toTypedArray()
            )
        )
    }

    override fun handleRemoveRecentHistoryGroup(searchTerm: String) {
        // We want to update the UI right away in response to user action without waiting for the IO.
        // First, dispatch actions that will clean up search groups in the two stores that have
        // metadata-related state.
        store.dispatch(HistoryMetadataAction.DisbandSearchGroupAction(searchTerm = searchTerm))
        homeStore.dispatch(HomeFragmentAction.DisbandSearchGroupAction(searchTerm = searchTerm))
        // Then, perform the expensive IO work of removing search groups from storage.
        scope.launch {
            storage.deleteHistoryMetadata(searchTerm)
        }
        metrics.track(Event.RecentSearchesGroupDeleted)
    }

    override fun handleRecentHistoryHighlightClicked(recentHistoryHighlight: RecentHistoryHighlight) {
        selectOrAddTabUseCase.invoke(recentHistoryHighlight.url)
        navController.navigate(R.id.browserFragment)
    }

    override fun handleRemoveRecentHistoryHighlight(url: String) {
        homeStore.dispatch(RemoveRecentHistoryHighlight(url))
        scope.launch {
            storage.deleteHistoryMetadataForUrl(url)
        }
    }

    @VisibleForTesting(otherwise = PRIVATE)
    fun dismissSearchDialogIfDisplayed() {
        if (navController.currentDestination?.id == R.id.searchDialogFragment) {
            navController.navigateUp()
        }
    }
}
