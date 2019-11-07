package org.mozilla.fenix.settings.search

import android.content.Context
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.item.SimpleBrowserMenuItem
import org.mozilla.fenix.R
import org.mozilla.fenix.theme.ThemeManager

class SearchEngineMenu(
    private val context: Context,
    private val onItemTapped: (Item) -> Unit = {}
) {
    sealed class Item {
        object Delete : Item()
    }

    val menuBuilder by lazy { BrowserMenuBuilder(menuItems) }

    private val menuItems by lazy {
        listOf(
            SimpleBrowserMenuItem(
                context.getString(R.string.search_engine_delete),
                textColorResource = ThemeManager.resolveAttribute(R.attr.destructive, context)
            ) {
                onItemTapped.invoke(Item.Delete)
            }
        )
    }
}
