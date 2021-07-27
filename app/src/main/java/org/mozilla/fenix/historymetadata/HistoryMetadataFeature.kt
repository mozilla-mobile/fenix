/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.historymetadata

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mozilla.components.concept.storage.HistoryMetadata
import mozilla.components.concept.storage.HistoryMetadataStorage
import mozilla.components.support.base.feature.LifecycleAwareFeature
import org.mozilla.fenix.home.HomeFragment
import org.mozilla.fenix.home.HomeFragmentAction
import org.mozilla.fenix.home.HomeFragmentStore

/**
 * View-bound feature that retrieves a list of history metadata and dispatches updates to the
 * [HomeFragmentStore].
 *
 * @param homeStore The [HomeFragmentStore] that holds the state of the [HomeFragment].
 * @param historyMetadataStorage The storage manages [HistoryMetadata].
 * @param scope The [CoroutineScope] used to retrieve a list of history metadata.
 * @param ioDispatcher The [CoroutineDispatcher] for performing read/write operations.
 */
class HistoryMetadataFeature(
    private val homeStore: HomeFragmentStore,
    private val historyMetadataStorage: HistoryMetadataStorage,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LifecycleAwareFeature {

    private var job: Job? = null

    override fun start() {
        job = scope.launch(ioDispatcher) {
            // For now, group the queried list of [HistoryMetadata] according to their search term.
            // This feature will later be used to generate more groups.
            val historyMetadata = historyMetadataStorage.getHistoryMetadataSince(Long.MIN_VALUE)
                .filter { it.totalViewTime > 0 && it.key.searchTerm != null }
                .groupBy { it.key.searchTerm!! }
                .map { (title, data) ->
                    HistoryMetadataGroup(
                        title = title,
                        historyMetadata = data
                    )
                }

            homeStore.dispatch(HomeFragmentAction.HistoryMetadataChange(historyMetadata))
        }
    }

    override fun stop() {
        job?.cancel()
    }
}
