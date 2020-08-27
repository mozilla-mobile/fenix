/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.bookmarks.viewholders

import androidx.appcompat.content.res.AppCompatResources
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.icons.IconRequest
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarkNodeType
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mozilla.fenix.library.SelectableWidgetSiteItem
import org.mozilla.fenix.library.bookmarks.BookmarkFragmentInteractor
import org.mozilla.fenix.library.bookmarks.BookmarkFragmentState
import org.mozilla.fenix.library.bookmarks.BookmarkPayload

class BookmarkNodeViewHolderTest {

    @MockK private lateinit var interactor: BookmarkFragmentInteractor
    @MockK(relaxed = true) private lateinit var siteItemView: SelectableWidgetSiteItem
    @MockK private lateinit var icons: BrowserIcons
    private lateinit var holder: BookmarkNodeViewHolder

    private val item = BookmarkNode(
        type = BookmarkNodeType.ITEM,
        guid = "456",
        parentGuid = "123",
        position = 0,
        title = "Mozilla",
        url = "https://www.mozilla.org",
        children = listOf()
    )
    private val folder = BookmarkNode(
        type = BookmarkNodeType.FOLDER,
        guid = "456",
        parentGuid = "123",
        position = 0,
        title = "Folder",
        url = null,
        children = listOf()
    )

    private val falsePayload = BookmarkPayload(
        titleChanged = false,
        urlChanged = false,
        selectedChanged = false,
        modeChanged = false,
        iconChanged = false
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockkStatic(AppCompatResources::class)
        every { AppCompatResources.getDrawable(any(), any()) } returns mockk(relaxed = true)
        every { icons.loadIntoView(siteItemView.widget.iconView, any()) } returns mockk()

        holder = BookmarkNodeViewHolder(siteItemView, interactor, icons)
    }

    @After
    fun teardown() {
        unmockkStatic(AppCompatResources::class)
    }

    @Test
    fun `binds views for unselected item`() {
        val mode = BookmarkFragmentState.Mode.Normal()
        holder.bind(item, mode, BookmarkPayload())

        verify {
            siteItemView.setSelectionInteractor(item, mode, interactor)
            siteItemView.widget.setText(item.title!!, item.url)
            siteItemView.attachMenu(any())
            siteItemView.changeSelected(false)
            icons.loadIntoView(siteItemView.widget.iconView, IconRequest(item.url!!))
        }
    }

    @Test
    fun `binds views for selected item for item`() {
        val mode = BookmarkFragmentState.Mode.Selecting(setOf(item))
        holder.bind(item, mode, BookmarkPayload())

        verify {
            siteItemView.setSelectionInteractor(item, mode, interactor)
            siteItemView.widget.setText(item.title!!, item.url)
            siteItemView.widget.removeSecondaryButton()
            siteItemView.changeSelected(true)
        }
    }

    @Test
    fun `bind with payload of no changes does not rebind views for item`() {
        holder.bind(
            item,
            BookmarkFragmentState.Mode.Normal(),
            falsePayload
        )

        verify(inverse = true) {
            siteItemView.widget.setText(item.title!!, item.url)
            siteItemView.attachMenu(any())
            siteItemView.widget.removeSecondaryButton()
            siteItemView.changeSelected(any())
        }
        verify { siteItemView.widget.iconView wasNot Called }
    }

    @Test
    fun `binding an item with a null title uses the url as the title for item`() {
        val item = item.copy(title = null)
        holder.bind(item, BookmarkFragmentState.Mode.Normal(), BookmarkPayload())

        verify { siteItemView.widget.setText(label = item.url!!, caption = null) }
    }

    @Test
    fun `binding an item with a blank title uses the url as the title for item`() {
        val item = item.copy(title = " ")
        holder.bind(item, BookmarkFragmentState.Mode.Normal(), BookmarkPayload())

        verify { siteItemView.widget.setText(label = item.url!!, caption = null) }
    }

    @Test
    fun `rebinds title if item title is null and the item url has changed for item`() {
        val item = item.copy(title = null)
        holder.bind(
            item,
            BookmarkFragmentState.Mode.Normal(),
            BookmarkPayload(
                titleChanged = false,
                urlChanged = true,
                selectedChanged = false,
                modeChanged = false,
                iconChanged = false
            )
        )

        verify { siteItemView.widget.setText(label = item.url!!, caption = null) }
    }

    @Test
    fun `rebinds title if item title is blank and the item url has changed for item`() {
        val item = item.copy(title = " ")
        holder.bind(
            item,
            BookmarkFragmentState.Mode.Normal(),
            BookmarkPayload(
                titleChanged = false,
                urlChanged = true,
                selectedChanged = false,
                modeChanged = false,
                iconChanged = false
            )
        )

        verify { siteItemView.widget.setText(label = item.url!!, caption = null) }
    }

    @Test
    fun `binds title and selected state for folder`() {
        holder.bind(folder, BookmarkFragmentState.Mode.Normal(), BookmarkPayload())

        verify {
            siteItemView.widget.setText(label = folder.title!!, caption = null)
            siteItemView.attachMenu(any())
            siteItemView.changeSelected(false)
        }

        holder.bind(folder, BookmarkFragmentState.Mode.Selecting(setOf(folder)), BookmarkPayload())

        verify {
            siteItemView.widget.setText(label = folder.title!!, caption = null)
            siteItemView.widget.removeSecondaryButton()
            siteItemView.changeSelected(true)
        }
    }

    @Test
    fun `bind with payload of no changes does not rebind views for folder`() {
        holder.bind(
            folder,
            BookmarkFragmentState.Mode.Normal(),
            falsePayload
        )

        verify(inverse = true) {
            siteItemView.widget.setText(label = folder.title!!, caption = null)
            siteItemView.attachMenu(any())
            siteItemView.widget.removeSecondaryButton()
            siteItemView.changeSelected(any())
        }
    }
}
