/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Class that represents the data for viewHolders that populate the history list in [HistoryAdapter]
 */
sealed class HistoryViewItem : Parcelable {

    @Parcelize
    data class HistoryItem(
        val data: History.Regular,
        val collapsed: Boolean = false
    ) : HistoryViewItem()

    @Parcelize
    data class HistoryGroupItem(
        val data: History.Group,
        val collapsed: Boolean = false
    ) : HistoryViewItem()

    @Parcelize
    data class TimeGroupHeader(
        val title: String,
        val timeGroup: HistoryItemTimeGroup,
        val collapsed: Boolean = false
    ) : HistoryViewItem()

    @Parcelize
    data class RecentlyClosedItem(
        val title: String,
        val body: String
    ) : HistoryViewItem()

    @Parcelize
    data class SyncedHistoryItem(
        val title: String
    ) : HistoryViewItem()

    @Parcelize
    data class EmptyHistoryItem(
        val title: String
    ) : HistoryViewItem()

    @Parcelize
    data class SignInHistoryItem(
        val instructionText: String
    ) : HistoryViewItem()

    @Parcelize
    data class TimeGroupSeparatorHistoryItem(
        val timeGroup: HistoryItemTimeGroup?
    ) : HistoryViewItem()

    @Parcelize
    object TopSeparatorHistoryItem : HistoryViewItem()
}
