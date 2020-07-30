/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.logins

import android.content.Context
import mozilla.components.browser.menu2.BrowserMenuController
import mozilla.components.concept.menu.candidate.HighPriorityHighlightEffect
import mozilla.components.concept.menu.candidate.MenuCandidate
import mozilla.components.concept.menu.candidate.TextMenuCandidate
import mozilla.components.concept.menu.candidate.TextStyle
import mozilla.components.support.ktx.android.content.getColorFromAttr
import org.mozilla.fenix.R

class SavedLoginsSortingStrategyMenu(
    private val context: Context,
    private val onItemTapped: (Item) -> Unit = {}
) {
    sealed class Item {
        object AlphabeticallySort : Item()
        object LastUsedSort : Item()
    }

    val menuController by lazy { BrowserMenuController() }

    private fun menuItems(itemToHighlight: Item): List<MenuCandidate> {
        val textStyle = TextStyle(
            color = context.getColorFromAttr(R.attr.primaryText)
        )

        val highlight = HighPriorityHighlightEffect(
            backgroundTint = context.getColorFromAttr(R.attr.colorControlHighlight)
        )

        return listOf(
            TextMenuCandidate(
                text = context.getString(R.string.saved_logins_sort_strategy_alphabetically),
                textStyle = textStyle,
                effect = if (itemToHighlight == Item.AlphabeticallySort) highlight else null
            ) {
                onItemTapped.invoke(Item.AlphabeticallySort)
            },
            TextMenuCandidate(
                text = context.getString(R.string.saved_logins_sort_strategy_last_used),
                textStyle = textStyle,
                effect = if (itemToHighlight == Item.LastUsedSort) highlight else null
            ) {
                onItemTapped.invoke(Item.LastUsedSort)
            }
        )
    }

    fun updateMenu(itemToHighlight: Item) {
        menuController.submitList(menuItems(itemToHighlight))
    }
}
