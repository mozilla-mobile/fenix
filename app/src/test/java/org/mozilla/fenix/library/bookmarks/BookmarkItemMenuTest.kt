/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.bookmarks

import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import mozilla.components.concept.menu.candidate.TextStyle
import mozilla.components.concept.storage.BookmarkNodeType
import mozilla.components.support.ktx.android.content.getColorFromAttr
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.FenixRobolectricTestRunner
import org.mozilla.fenix.library.bookmarks.BookmarkItemMenu.Item

@RunWith(FenixRobolectricTestRunner::class)
class BookmarkItemMenuTest {

    private lateinit var context: Context
    private lateinit var menu: BookmarkItemMenu
    private var lastItemTapped: Item? = null

    @Before
    fun setup() {
        context = ContextThemeWrapper(testContext, R.style.NormalTheme)
        menu = BookmarkItemMenu(context) {
            lastItemTapped = it
        }
    }

    @Test
    fun `delete item has special styling`() {
        val deleteItem = menu.menuItems(BookmarkNodeType.SEPARATOR).last()
        assertEquals("Delete", deleteItem.text)
        assertEquals(
            TextStyle(color = context.getColorFromAttr(R.attr.textWarning)),
            deleteItem.textStyle
        )

        deleteItem.onClick()

        assertEquals(Item.Delete, lastItemTapped)
    }

    @Test
    fun `edit item appears for folders`() {
        val folderItems = menu.menuItems(BookmarkNodeType.FOLDER)
        assertEquals(2, folderItems.size)
        val (edit, delete) = folderItems

        assertEquals("Edit", edit.text)
        edit.onClick()

        assertEquals(Item.Edit, lastItemTapped)
        assertEquals("Delete", delete.text)
    }

    @Test
    fun `all item appears for sites`() {
        val siteItems = menu.menuItems(BookmarkNodeType.ITEM)
        assertEquals(6, siteItems.size)
        val (edit, copy, share, openInNewTab, openInPrivateTab, delete) = siteItems

        assertEquals("Edit", edit.text)
        assertEquals("Copy", copy.text)
        assertEquals("Share", share.text)
        assertEquals("Open in new tab", openInNewTab.text)
        assertEquals("Open in private tab", openInPrivateTab.text)
        assertEquals("Delete", delete.text)

        edit.onClick()
        assertEquals(Item.Edit, lastItemTapped)

        copy.onClick()
        assertEquals(Item.Copy, lastItemTapped)

        share.onClick()
        assertEquals(Item.Share, lastItemTapped)

        openInNewTab.onClick()
        assertEquals(Item.OpenInNewTab, lastItemTapped)

        openInPrivateTab.onClick()
        assertEquals(Item.OpenInPrivateTab, lastItemTapped)

        delete.onClick()
        assertEquals(Item.Delete, lastItemTapped)
    }

    private operator fun <T> List<T>.component6(): T {
        return get(5)
    }
}
