/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol.viewholders

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observer
import kotlinx.android.synthetic.main.tab_header.view.*
import mozilla.components.browser.menu.BrowserMenu
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.item.SimpleBrowserMenuItem
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.increaseTapArea
import org.mozilla.fenix.home.sessioncontrol.SessionControlAction
import org.mozilla.fenix.home.sessioncontrol.TabAction
import org.mozilla.fenix.home.sessioncontrol.onNext

class TabHeaderViewHolder(
    view: View,
    private val actionEmitter: Observer<SessionControlAction>
) : RecyclerView.ViewHolder(view) {
    private var isPrivate = false
    private var tabsMenu: TabHeaderMenu

    init {
        tabsMenu = TabHeaderMenu(view.context) {
            when (it) {
                is TabHeaderMenu.Item.Share -> actionEmitter.onNext(TabAction.ShareTabs)
                is TabHeaderMenu.Item.CloseAll -> actionEmitter.onNext(TabAction.CloseAll(isPrivate))
                is TabHeaderMenu.Item.SaveToCollection -> actionEmitter.onNext(
                    TabAction.SaveTabGroup(
                        null
                    )
                )
            }
        }
        view.apply {
            val headerTextResourceId =
                if (isPrivate) R.string.tabs_header_private_title else R.string.tab_header_label
            header_text.text = context.getString(headerTextResourceId)

            add_tab_button.increaseTapArea(addTabButtonIncreaseDps)
            add_tab_button.setOnClickListener {
                actionEmitter.onNext(TabAction.Add)
            }

            tabs_overflow_button.run {
                increaseTapArea(overflowButtonIncreaseDps)
                setOnClickListener {
                    tabsMenu.menuBuilder
                        .build(view.context)
                        .show(anchor = it, orientation = BrowserMenu.Orientation.DOWN)
                }
            }
        }
    }

    class TabHeaderMenu(
        private val context: Context,
        private val onItemTapped: (Item) -> Unit = {}
    ) {
        sealed class Item {
            object CloseAll : Item()
            object Share : Item()
            object SaveToCollection : Item()
        }

        val menuBuilder by lazy { BrowserMenuBuilder(menuItems) }

        private val menuItems by lazy {
            listOf(
                SimpleBrowserMenuItem(
                    context.getString(R.string.tabs_menu_close_all_tabs)
                ) {
                    onItemTapped.invoke(Item.CloseAll)
                },
                SimpleBrowserMenuItem(
                    context.getString(R.string.tabs_menu_share_tabs)
                ) {
                    onItemTapped.invoke(Item.Share)
                },
                SimpleBrowserMenuItem(
                    context.getString(R.string.tabs_menu_save_to_collection)
                ) {
                    onItemTapped.invoke(Item.SaveToCollection)
                }
            )
        }
    }

    companion object {
        const val LAYOUT_ID = R.layout.tab_header

        const val addTabButtonIncreaseDps = 8
        const val overflowButtonIncreaseDps = 8
    }
}
