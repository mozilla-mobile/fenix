/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.bookmarks

import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarkNodeType
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner

@RunWith(FenixRobolectricTestRunner::class)
class UtilsKtTest {
    @Test
    fun `friendly root titles`() {
        val url = BookmarkNode(
            BookmarkNodeType.ITEM,
            "456",
            "folder",
            0,
            "Mozilla",
            "http://mozilla.org",
            null
        )
        assertEquals("Mozilla", friendlyRootTitle(testContext, url))

        val folder = BookmarkNode(
            BookmarkNodeType.FOLDER,
            "456",
            "folder",
            0,
            "Folder",
            null,
            null
        )
        assertEquals("Folder", friendlyRootTitle(testContext, folder))

        val root = folder.copy(guid = "root________", title = "root")
        assertEquals("Bookmarks", friendlyRootTitle(testContext, root, withMobileRoot = true))
        assertEquals("Desktop Bookmarks", friendlyRootTitle(testContext, root, withMobileRoot = false))

        val mobileRoot = folder.copy(guid = "mobile______", title = "mobile")
        assertEquals("Bookmarks", friendlyRootTitle(testContext, mobileRoot, withMobileRoot = true))
        assertEquals("mobile", friendlyRootTitle(testContext, mobileRoot, withMobileRoot = false))

        val menuRoot = folder.copy(guid = "menu________", title = "menu")
        assertEquals("Bookmarks Menu", friendlyRootTitle(testContext, menuRoot, withMobileRoot = true))
        assertEquals("Bookmarks Menu", friendlyRootTitle(testContext, menuRoot, withMobileRoot = false))

        val toolbarRoot = folder.copy(guid = "toolbar_____", title = "toolbar")
        assertEquals("Bookmarks Toolbar", friendlyRootTitle(testContext, toolbarRoot, withMobileRoot = true))
        assertEquals("Bookmarks Toolbar", friendlyRootTitle(testContext, toolbarRoot, withMobileRoot = false))

        val unfiledRoot = folder.copy(guid = "unfiled_____", title = "unfiled")
        assertEquals("Other Bookmarks", friendlyRootTitle(testContext, unfiledRoot, withMobileRoot = true))
        assertEquals("Other Bookmarks", friendlyRootTitle(testContext, unfiledRoot, withMobileRoot = false))

        val almostRoot = folder.copy(guid = "notRoot________", title = "root")
        assertEquals("root", friendlyRootTitle(testContext, almostRoot, withMobileRoot = true))
        assertEquals("root", friendlyRootTitle(testContext, almostRoot, withMobileRoot = false))
    }

    @Test
    fun `flatNodeList various cases`() {
        val url = BookmarkNode(
            BookmarkNodeType.ITEM,
            "456",
            "folder",
            0,
            "Mozilla",
            "http://mozilla.org",
            null
        )
        val url2 = BookmarkNode(
            BookmarkNodeType.ITEM,
            "8674",
            "folder2",
            0,
            "Mozilla",
            "http://mozilla.org",
            null
        )
        assertEquals(emptyList<BookmarkNodeWithDepth>(), url.flatNodeList(null))

        val root = BookmarkNode(
            BookmarkNodeType.FOLDER,
            "root",
            null,
            0,
            "root",
            null,
            null
        )
        assertEquals(listOf(BookmarkNodeWithDepth(0, root, null)), root.flatNodeList(null))
        assertEquals(emptyList<BookmarkNodeWithDepth>(), root.flatNodeList("root"))

        val folder = BookmarkNode(
            BookmarkNodeType.FOLDER,
            "folder",
            root.guid,
            0,
            "folder",
            null,
            listOf(url)
        )

        val folder3 = BookmarkNode(
            BookmarkNodeType.FOLDER,
            "folder3",
            "folder2",
            0,
            "folder3",
            null,
            null
        )

        val folder2 = BookmarkNode(
            BookmarkNodeType.FOLDER,
            "folder2",
            root.guid,
            0,
            "folder2",
            null,
            listOf(folder3, url2)
        )

        val rootWithChildren = root.copy(children = listOf(folder, folder2))
        assertEquals(
            listOf(
                BookmarkNodeWithDepth(0, rootWithChildren, null),
                BookmarkNodeWithDepth(1, folder, "root"),
                BookmarkNodeWithDepth(1, folder2, "root"),
                BookmarkNodeWithDepth(2, folder3, "folder2")
            ), rootWithChildren.flatNodeList(null)
        )

        assertEquals(
            listOf(
                BookmarkNodeWithDepth(0, rootWithChildren, null),
                BookmarkNodeWithDepth(1, folder, "root")
            ), rootWithChildren.flatNodeList(excludeSubtreeRoot = "folder2")
        )
    }
}
