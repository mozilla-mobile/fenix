/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.recentbookmarks

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import mozilla.components.support.test.rule.MainCoroutineRule
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.components.bookmarks.BookmarksUseCase
import org.mozilla.fenix.home.HomeFragmentStore

@OptIn(ExperimentalCoroutinesApi::class)
class RecentBookmarksFeatureTest {

    private val homeStore: HomeFragmentStore = mockk(relaxed = true)
    private val bookmarksUseCases: BookmarksUseCase = mockk(relaxed = true)
    private val scope = TestCoroutineScope()
    private val testDispatcher = TestCoroutineDispatcher()

    @get:Rule
    val coroutinesTestRule = MainCoroutineRule(testDispatcher)

    @Before
    fun setup() {
        coEvery { bookmarksUseCases.retrieveRecentBookmarks() }.coAnswers { listOf() }
    }

    @After
    fun cleanUp() {
        scope.cleanupTestCoroutines()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `GIVEN no recently saved bookmarks WHEN the feature starts THEN fetch list of bookmarks AND notify the store`() = testDispatcher.runBlockingTest {
        val feature = spyk(RecentBookmarksFeature(
            homeStore,
            bookmarksUseCases,
            scope
        ))

        feature.start()

        testDispatcher.advanceUntilIdle()

        coVerify {
            bookmarksUseCases.retrieveRecentBookmarks()
            homeStore.dispatch(any())
        }
    }

    @Test
    fun `WHEN the feature is destroyed THEN the job is cancelled`() {
        val feature = spyk(RecentBookmarksFeature(
            homeStore,
            bookmarksUseCases,
            scope
        ))

        assertNull(feature.job)

        feature.start()

        assertNotNull(feature.job)

        feature.stop()

        verify(exactly = 1) { feature.stop() }
    }
}
