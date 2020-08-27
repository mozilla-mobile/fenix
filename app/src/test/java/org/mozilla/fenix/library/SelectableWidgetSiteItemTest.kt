/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library

import android.view.LayoutInflater
import android.view.View
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import mozilla.components.concept.menu.MenuController
import mozilla.components.concept.menu.Orientation
import mozilla.components.ui.widgets.WidgetSiteItemView
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mozilla.fenix.R

class SelectableWidgetSiteItemTest {

    @MockK private lateinit var inflater: LayoutInflater
    @MockK(relaxed = true) private lateinit var siteItemView: WidgetSiteItemView
    @MockK(relaxed = true) private lateinit var checkmark: View
    private lateinit var selectableWidget: SelectableWidgetSiteItem

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        every { inflater.inflate(R.layout.site_list_item_selectable, any(), any()) } returns siteItemView
        every { inflater.inflate(R.layout.checkbox_item, any(), any()) } returns checkmark

        selectableWidget = SelectableWidgetSiteItem(mockk(), inflater)
    }

    @Test
    fun `changeSelected adjusts the visibility of the checkmark`() {
        selectableWidget.changeSelected(isSelected = true)
        verify { checkmark.visibility = View.VISIBLE }

        selectableWidget.changeSelected(isSelected = false)
        verify { checkmark.visibility = View.GONE }
    }

    @Test
    fun `attachMenu displays a menu button`() {
        val menuController = mockk<MenuController> {
            every { show(any(), orientation = Orientation.DOWN) } returns mockk()
        }
        selectableWidget.attachMenu(menuController)

        val onClick = slot<(View) -> Unit>()
        verify {
            siteItemView.setSecondaryButton(
                icon = R.drawable.ic_menu,
                contentDescription = R.string.content_description_menu,
                onClickListener = capture(onClick)
            )
        }

        val secondaryButton = mockk<View>()
        onClick.captured(secondaryButton)
        verify { menuController.show(secondaryButton, orientation = Orientation.DOWN) }
    }

    @Test
    fun `setSelectionInteractor sets up onClick listener`() {
        val item = "test"
        val interactor = mockk<SelectionInteractor<String>>(relaxed = true)
        val onClick = slot<View.OnClickListener>()
        every { siteItemView.setOnClickListener(capture(onClick)) } just Runs

        // nothing selected
        selectableWidget.setSelectionInteractor(
            item,
            MockSelectionHolder(emptySet()),
            interactor
        )
        onClick.captured.onClick(siteItemView)
        verify { interactor.open(item) }

        // item selected
        selectableWidget.setSelectionInteractor(
            item,
            MockSelectionHolder(setOf(item, "other")),
            interactor
        )
        onClick.captured.onClick(siteItemView)
        verify { interactor.deselect(item) }

        // item not selected
        selectableWidget.setSelectionInteractor(
            item,
            MockSelectionHolder(setOf("other")),
            interactor
        )
        onClick.captured.onClick(siteItemView)
        verify { interactor.select(item) }
    }

    @Test
    fun `setSelectionInteractor sets up onLongClick listener`() {
        val item = 10
        val interactor = mockk<SelectionInteractor<Int>>(relaxed = true)
        val onLongClick = slot<View.OnLongClickListener>()
        every { siteItemView.setOnLongClickListener(capture(onLongClick)) } just Runs

        // something selected
        selectableWidget.setSelectionInteractor(
            item,
            MockSelectionHolder(setOf(item, 42)),
            interactor
        )
        assertFalse(onLongClick.captured.onLongClick(siteItemView))
        verify { interactor wasNot Called }

        // nothing selected
        selectableWidget.setSelectionInteractor(
            item,
            MockSelectionHolder(emptySet()),
            interactor
        )
        assertTrue(onLongClick.captured.onLongClick(siteItemView))
        verify { interactor.select(item) }
    }

    @Test
    fun `setSelectionInteractor sets up icon click listener`() {
        val item = mockk<Any>()
        val interactor = mockk<SelectionInteractor<Any>>(relaxed = true)
        val onClick = slot<View.OnClickListener>()
        every { siteItemView.iconView.setOnClickListener(capture(onClick)) } just Runs

        // item selected
        selectableWidget.setSelectionInteractor(
            item,
            MockSelectionHolder(setOf(mockk(), item)),
            interactor
        )
        onClick.captured.onClick(siteItemView)
        verify { interactor.deselect(item) }

        // item not selected
        selectableWidget.setSelectionInteractor(
            item,
            MockSelectionHolder(setOf(mockk())),
            interactor
        )
        onClick.captured.onClick(siteItemView)
        verify { interactor.select(item) }
    }

    private class MockSelectionHolder<T>(
        override val selectedItems: Set<T>
    ) : SelectionHolder<T>
}
