/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.historymetadata.interactor

import io.mockk.mockk
import io.mockk.verify
import mozilla.components.concept.storage.DocumentType
import mozilla.components.concept.storage.HistoryMetadata
import mozilla.components.concept.storage.HistoryMetadataKey
import org.junit.Before
import org.junit.Test
import org.mozilla.fenix.historymetadata.RecentlyVisitedItem.RecentHistoryGroup
import org.mozilla.fenix.historymetadata.RecentlyVisitedItem.RecentHistoryHighlight
import org.mozilla.fenix.historymetadata.controller.HistoryMetadataController
import org.mozilla.fenix.home.pocket.PocketStoriesController
import org.mozilla.fenix.home.recentbookmarks.controller.RecentBookmarksController
import org.mozilla.fenix.home.recenttabs.controller.RecentTabController
import org.mozilla.fenix.home.sessioncontrol.DefaultSessionControlController
import org.mozilla.fenix.home.sessioncontrol.SessionControlInteractor

class HistoryMetadataInteractorTest {
    private val defaultSessionControlController: DefaultSessionControlController =
        mockk(relaxed = true)
    private val recentTabController: RecentTabController = mockk(relaxed = true)
    private val recentBookmarksController: RecentBookmarksController = mockk(relaxed = true)
    private val pocketStoriesController: PocketStoriesController = mockk(relaxed = true)
    private val historyMetadataController: HistoryMetadataController = mockk(relaxed = true)

    private lateinit var interactor: SessionControlInteractor

    @Before
    fun setup() {
        interactor = SessionControlInteractor(
            defaultSessionControlController,
            recentTabController,
            recentBookmarksController,
            historyMetadataController,
            pocketStoriesController
        )
    }

    @Test
    fun handleRecentHistoryGroupClicked() {
        val historyGroup =
            RecentHistoryGroup(
                title = "mozilla",
                historyMetadata = listOf(
                    HistoryMetadata(
                        key = HistoryMetadataKey("http://www.mozilla.com", null, null),
                        title = "mozilla",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        totalViewTime = 10,
                        documentType = DocumentType.Regular,
                        previewImageUrl = null
                    )
                )
            )

        interactor.onRecentHistoryGroupClicked(historyGroup)
        verify {
            historyMetadataController.handleRecentHistoryGroupClicked(historyGroup)
        }
    }

    @Test
    fun handleHistoryShowAllClicked() {
        interactor.onHistoryShowAllClicked()
        verify { historyMetadataController.handleHistoryShowAllClicked() }
    }

    @Test
    fun onRemoveRecentHistoryGroup() {
        val historyMetadataKey = HistoryMetadataKey(
            "http://www.mozilla.com",
            "mozilla",
            null
        )

        val historyGroup =
            RecentHistoryGroup(
                title = "mozilla",
                historyMetadata = listOf(
                    HistoryMetadata(
                        key = historyMetadataKey,
                        title = "mozilla",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        totalViewTime = 10,
                        documentType = DocumentType.Regular,
                        previewImageUrl = null
                    )
                )
            )

        interactor.onRemoveRecentHistoryGroup(historyGroup.title)

        verify {
            historyMetadataController.handleRemoveRecentHistoryGroup(historyGroup.title)
        }
    }

    @Test
    fun onRecentHistoryHighlightClicked() {
        val historyHighlight: RecentHistoryHighlight = mockk()

        interactor.onRecentHistoryHighlightClicked(historyHighlight)

        verify { historyMetadataController.handleRecentHistoryHighlightClicked(historyHighlight) }
    }

    @Test
    fun onRemoveRecentHistoryHighlight() {
        interactor.onRemoveRecentHistoryHighlight("url")

        verify { historyMetadataController.handleRemoveRecentHistoryHighlight("url") }
    }
}
