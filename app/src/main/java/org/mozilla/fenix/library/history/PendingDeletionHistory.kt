/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import mozilla.components.concept.storage.HistoryMetadataKey

/**
 * Wrapper for the data of the history item that has been marked for removal. Undo snackbar delays
 * the actual removal, while this class is used to match History items that should be hidden in the
 * UI.
 */
sealed class PendingDeletionHistory {

    abstract val visitedAt: Long
    abstract val timeGroup: HistoryItemTimeGroup

    /**
     * This class represents a single, separate item in the history list.
     */
    data class Item(
        override val visitedAt: Long,
        override val timeGroup: HistoryItemTimeGroup,
        val url: String
    ) : PendingDeletionHistory()

    /**
     * This class represents a group in the history list.
     */
    data class Group(
        override val visitedAt: Long,
        override val timeGroup: HistoryItemTimeGroup,
        val historyMetadata: List<MetaData>
    ) : PendingDeletionHistory()

    /**
     * This class represents an item inside a group in the group history list
     */
    data class MetaData(
        override val visitedAt: Long,
        override val timeGroup: HistoryItemTimeGroup,
        val key: HistoryMetadataKey
    ) : PendingDeletionHistory()
}

/**
 * Maps an instance of [History] to an instance of [PendingDeletionHistory].
 */
fun History.toPendingDeletionHistory(): PendingDeletionHistory {
    return when (this) {
        is History.Regular -> PendingDeletionHistory.Item(
            visitedAt = visitedAt,
            timeGroup = historyTimeGroup,
            url = url
        )
        is History.Group -> PendingDeletionHistory.Group(
            visitedAt = visitedAt,
            timeGroup = historyTimeGroup,
            historyMetadata = items.map { historyMetadata ->
                PendingDeletionHistory.MetaData(
                    historyMetadata.visitedAt,
                    historyMetadata.historyTimeGroup,
                    historyMetadata.historyMetadataKey
                )
            }
        )
        is History.Metadata -> PendingDeletionHistory.MetaData(visitedAt, historyTimeGroup, historyMetadataKey)
    }
}
