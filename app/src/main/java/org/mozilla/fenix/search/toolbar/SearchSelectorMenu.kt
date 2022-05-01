/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.search.toolbar

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.appcompat.content.res.AppCompatResources
import mozilla.components.browser.menu2.BrowserMenuController
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.concept.menu.MenuController
import mozilla.components.concept.menu.candidate.DrawableMenuIcon
import mozilla.components.concept.menu.candidate.TextMenuCandidate
import mozilla.components.support.ktx.android.content.getColorFromAttr
import org.mozilla.fenix.R

typealias MozSearchEngine = SearchEngine

/**
 * A popup menu composed of [SearchSelectorMenu.Item] objects.
 *
 * @property context [Context] used for various Android interactions.
 * @property interactor [ToolbarInteractor] for handling menu item interactions.
 */
class SearchSelectorMenu(
    private val context: Context,
    private val interactor: ToolbarInteractor
) {

    /**
     * Items that will appear in the search selector menu.
     */
    sealed class Item {
        /**
         * The menu item to navigate to the search settings.
         */
        object SearchSettings : Item()

        /**
         * The menu item to display a search engine.
         *
         * @param searchEngine The [SearchEngine] that was selected.
         */
        data class SearchEngine(val searchEngine: MozSearchEngine) : Item()
    }

    val menuController: MenuController by lazy { BrowserMenuController() }

    @VisibleForTesting
    internal fun menuItems(): List<TextMenuCandidate> {
        return listOf(
            TextMenuCandidate(
                text = context.getString(R.string.search_settings_menu_item),
                start = DrawableMenuIcon(
                    drawable = AppCompatResources.getDrawable(
                        context,
                        R.drawable.mozac_ic_settings
                    ),
                    tint = context.getColorFromAttr(R.attr.textPrimary)
                )
            ) {
                interactor.onMenuItemTapped(Item.SearchSettings)
            }
        )
    }
}
