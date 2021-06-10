/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.recentbookmarks

import io.mockk.mockk
import io.mockk.verify
import mozilla.components.concept.storage.BookmarkNode
import org.junit.Before
import org.junit.Test
import org.mozilla.fenix.home.recentbookmarks.controller.DefaultRecentBookmarksController
import org.mozilla.fenix.home.recentbookmarks.interactor.DefaultRecentBookmarksInteractor

class DefaultRecentBookmarksInteractorTest {

    private val controller: DefaultRecentBookmarksController = mockk(relaxed = true)

    private lateinit var interactor: DefaultRecentBookmarksInteractor

    @Before
    fun setup() {
        interactor = DefaultRecentBookmarksInteractor(controller)
    }

    @Test
    fun `WHEN a recently saved bookmark is clicked THEN the selected bookmark is handled`() {
        val bookmark = BookmarkNode(
            type = mockk(),
            guid = mockk(),
            parentGuid = null,
            position = null,
            title = null,
            url = null,
            children = null
        )

        interactor.onRecentBookmarkClicked(bookmark)
        verify { controller.handleBookmarkClicked(bookmark) }
    }

    @Test
    fun `WHEN Show All recently saved bookmarks button is clicked THEN the click is handled`() {
        interactor.onShowAllBookmarksClicked()
        verify { controller.handleShowAllBookmarksClicked() }
    }
}
