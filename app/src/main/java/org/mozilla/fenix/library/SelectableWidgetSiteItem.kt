/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import mozilla.components.concept.menu.MenuController
import mozilla.components.concept.menu.Orientation
import mozilla.components.ui.widgets.WidgetSiteItemView
import org.mozilla.fenix.R

/**
 * Interactor for items that can be selected on the bookmarks and history screens.
 */
interface SelectionInteractor<T> {
    /**
     * Called when an item is tapped to open it.
     * @param item the tapped item to open.
     */
    fun open(item: T)

    /**
     * Called when an item is long pressed and selection mode is started,
     * or when selection mode has already started an an item is tapped.
     * @param item the item to select.
     */
    fun select(item: T)

    /**
     * Called when a selected item is tapped in selection mode and should no longer be selected.
     * @param item the item to deselect.
     */
    fun deselect(item: T)
}

interface SelectionHolder<T> {
    val selectedItems: Set<T>
}

/**
 * Shared UI widget that wraps [WidgetSiteItemView].
 * Used for showing a website in a list of websites, while allowing items to be checked.
 */
class SelectableWidgetSiteItem(
    root: ViewGroup,
    inflater: LayoutInflater = LayoutInflater.from(root.context)
) {

    val context: Context get() = widget.context

    /**
     * The inner [WidgetSiteItemView] that is wrapped by this class.
     */
    val widget =
        inflater.inflate(R.layout.site_list_item_selectable, root, false) as WidgetSiteItemView
    private val checkmark: View =
        inflater.inflate(R.layout.checkbox_item, widget, false)

    init {
        widget.addIconOverlay(checkmark)
    }

    /**
     * Show a check mark above the icon if [isSelected] is true.
     */
    fun changeSelected(isSelected: Boolean) {
        checkmark.isVisible = isSelected
    }

    /**
     * Attaches a menu to this item and displays a 3-dot menu button.
     *
     * Only call this method if you want to show a menu. The button is hidden by default.
     * Note that calls to [WidgetSiteItemView.setSecondaryButton] will replace the menu.
     *
     * @param menuController Menu controller to handle displaying a menu.
     */
    fun attachMenu(menuController: MenuController) {
        widget.setSecondaryButton(
            icon = R.drawable.ic_menu,
            contentDescription = R.string.content_description_menu
        ) {
            menuController.show(
                anchor = it,
                orientation = Orientation.DOWN
            )
        }
    }

    /**
     * Sets up selection click listeners.
     *
     * Only call this method if you want the user to select items in the list.
     *
     * @param item The item that is represented by this widget.
     * @param holder Class that holds the currently selected items.
     * @param interactor Interactor that handles selection events.
     */
    fun <T> setSelectionInteractor(
        item: T,
        holder: SelectionHolder<T>,
        interactor: SelectionInteractor<T>
    ) {
        widget.setOnClickListener {
            val selected = holder.selectedItems
            when {
                selected.isEmpty() -> interactor.open(item)
                item in selected -> interactor.deselect(item)
                else -> interactor.select(item)
            }
        }

        widget.setOnLongClickListener {
            if (holder.selectedItems.isEmpty()) {
                interactor.select(item)
                true
            } else {
                false
            }
        }

        widget.iconView.setOnClickListener {
            if (item in holder.selectedItems) {
                interactor.deselect(item)
            } else {
                interactor.select(item)
            }
        }
    }
}
