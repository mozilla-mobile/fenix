/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.recentbookmarks

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.support.test.libstate.ext.waitUntilIdle
import mozilla.components.support.test.middleware.CaptureActionsMiddleware
import mozilla.components.support.test.rule.MainCoroutineRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.components.bookmarks.BookmarksUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class RecentBookmarksFeatureTest {

    private val middleware = CaptureActionsMiddleware<AppState, AppAction>()
    private val appStore = AppStore(middlewares = listOf(middleware))
    private val bookmarksUseCases: BookmarksUseCase = mockk(relaxed = true)
    private val bookmark = RecentBookmark(
        title = null,
        url = "https://www.example.com",
        previewImageUrl = null
    )

    @get:Rule
    val coroutinesTestRule = MainCoroutineRule()
    private val testDispatcher = coroutinesTestRule.testDispatcher

    @Before
    fun setup() {
        coEvery { bookmarksUseCases.retrieveRecentBookmarks() }.coAnswers { listOf(bookmark) }
    }

    @Test
    fun `GIVEN no recent bookmarks WHEN feature starts THEN fetch bookmarks and notify store`() =
        testDispatcher.runBlockingTest {
            val feature = RecentBookmarksFeature(
                appStore,
                bookmarksUseCases,
                CoroutineScope(testDispatcher),
                testDispatcher
            )

            assertEquals(emptyList<BookmarkNode>(), appStore.state.recentBookmarks)

            feature.start()

            testDispatcher.advanceUntilIdle()
            appStore.waitUntilIdle()

            coVerify {
                bookmarksUseCases.retrieveRecentBookmarks()
            }

            middleware.assertLastAction(AppAction.RecentBookmarksChange::class) {
                assertEquals(listOf(bookmark), it.recentBookmarks)
            }
        }
}
