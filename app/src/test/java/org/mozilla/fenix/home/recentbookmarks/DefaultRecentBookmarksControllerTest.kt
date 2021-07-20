/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.recentbookmarks

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import mozilla.appservices.places.BookmarkRoot
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarkNodeType
import mozilla.components.support.test.rule.MainCoroutineRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.home.HomeFragmentDirections
import org.mozilla.fenix.home.recentbookmarks.controller.DefaultRecentBookmarksController

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultRecentBookmarksControllerTest {

    private val testDispatcher = TestCoroutineDispatcher()

    @get:Rule
    val coroutinesTestRule = MainCoroutineRule(testDispatcher)

    private val activity: HomeActivity = mockk(relaxed = true)
    private val navController: NavController = mockk(relaxUnitFun = true)
    private lateinit var controller: DefaultRecentBookmarksController

    @Before
    fun setup() {
        every { activity.openToBrowserAndLoad(any(), any(), any()) } just Runs
        every { navController.currentDestination } returns mockk {
            every { id } returns R.id.homeFragment
        }

        controller = spyk(DefaultRecentBookmarksController(
            activity = activity,
            navController = navController
        ))
    }

    @After
    fun cleanUp() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `WHEN a recently saved bookmark is clicked THEN the selected bookmark is opened`() {
        val bookmark = BookmarkNode(
            type = BookmarkNodeType.ITEM,
            guid = "guid#${Math.random() * 1000}",
            parentGuid = null,
            position = null,
            title = null,
            url = "https://www.example.com",
            children = null
        )

        controller.handleBookmarkClicked(bookmark)

        verify {
            activity.openToBrowserAndLoad(
                searchTermOrURL = bookmark.url!!,
                newTab = true,
                from = BrowserDirection.FromHome
            )
        }
    }

    @Test
    fun `WHEN show all recently saved bookmark is clicked THEN the bookmarks root is opened`() {
        controller.handleShowAllBookmarksClicked()

        val directions = HomeFragmentDirections.actionGlobalBookmarkFragment(BookmarkRoot.Mobile.id)
        verify { navController.navigate(directions, any<NavOptions>()) }
    }
}
