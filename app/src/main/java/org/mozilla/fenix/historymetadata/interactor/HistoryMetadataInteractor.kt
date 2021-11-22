/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.historymetadata.interactor

import org.mozilla.fenix.historymetadata.RecentlyVisitedItem.RecentHistoryGroup
import org.mozilla.fenix.historymetadata.RecentlyVisitedItem.RecentHistoryHighlight

/**
 * Interface for history metadata related actions in the Home screen.
 */
interface HistoryMetadataInteractor {

    /**
     * Shows the history fragment. Called when a user clicks on the "Show all" button besides the
     * history metadata header.
     */
    fun onHistoryShowAllClicked()

    /**
     * Navigates to the history metadata group fragment to display the group. Called when a user
     * clicks on a history metadata group.
     *
     * @param recentHistoryGroup The [RecentHistoryGroup] to toggle its expanded state.
     */
    fun onRecentHistoryGroupClicked(recentHistoryGroup: RecentHistoryGroup)

    /**
     * Removes a history metadata group with the given search term from the homescreen.
     *
     * @param searchTerm The search term to be removed.
     */
    fun onRemoveRecentHistoryGroup(searchTerm: String)

    /**
     * Callback for when a [RecentHistoryHighlight] is clicked.
     *
     * @param recentHistoryHighlight The just clicked [RecentHistoryHighlight].
     */
    fun onRecentHistoryHighlightClicked(recentHistoryHighlight: RecentHistoryHighlight)

    /**
     * Removes a history highlight with the given [url] from the homescreen.
     *
     * @param url [RecentHistoryHighlight.url] of the item to remove.
     */
    fun onRemoveRecentHistoryHighlight(url: String)
}
