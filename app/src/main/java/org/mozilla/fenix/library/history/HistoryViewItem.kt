/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * The class represents the items types used by [HistoryAdapter] to populate the list.
 * It contains the data for viewHolders. Subclasses match the variety of viewHolders.
 */
sealed class HistoryViewItem : Parcelable {

    /**
     * A class representing a regular history record in the history and synced history lists.
     */
    @Parcelize
    data class HistoryItem(
        val data: History.Regular,
        val collapsed: Boolean = false
    ) : HistoryViewItem()

    /**
     * A class representing a search group (a group of history items) in the history list.
     */
    @Parcelize
    data class HistoryGroupItem(
        val data: History.Group,
        val collapsed: Boolean = false
    ) : HistoryViewItem()

    /**
     * A class representing a timeGroup in the history and synced history lists.
     */
    @Parcelize
    data class TimeGroupHeader(
        val title: String,
        val timeGroup: HistoryItemTimeGroup,
        val collapsed: Boolean = false
    ) : HistoryViewItem()

    /**
     * A class representing a recently closed button in the history list.
     */
    @Parcelize
    data class RecentlyClosedItem(
        val title: String,
        val body: String
    ) : HistoryViewItem()

    /**
     * A class representing a synced history button in the history list.
     */
    @Parcelize
    data class SyncedHistoryItem(
        val title: String
    ) : HistoryViewItem()

    /**
     * A class representing empty state in history and synced history screens.
     */
    @Parcelize
    data class EmptyHistoryItem(
        val title: String
    ) : HistoryViewItem()

    /**
     * A class representing a sign-in window inside the synced history screen.
     */
    @Parcelize
    data class SignInHistoryItem(
        val instructionText: String
    ) : HistoryViewItem()

    /**
     * A class representing an extra space that timeGroup items have above them when they are
     * not in a collapsed state.
     */
    @Parcelize
    data class TimeGroupSeparatorHistoryItem(
        val timeGroup: HistoryItemTimeGroup?
    ) : HistoryViewItem()

    /**
     * A class representing a space at the top of history and synced history lists.
     */
    @Parcelize
    object TopSeparatorHistoryItem : HistoryViewItem()
}
