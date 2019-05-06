/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol.viewholders

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observer
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.collection_home_list_row.*
import kotlinx.android.synthetic.main.collection_home_list_row.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import mozilla.components.browser.menu.BrowserMenu
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.item.SimpleBrowserMenuItem
import org.mozilla.fenix.DefaultThemeManager
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.increaseTapArea
import org.mozilla.fenix.home.sessioncontrol.CollectionAction
import org.mozilla.fenix.home.sessioncontrol.SessionControlAction
import org.mozilla.fenix.home.sessioncontrol.TabCollection
import org.mozilla.fenix.home.sessioncontrol.onNext
import org.mozilla.fenix.utils.Settings
import kotlin.coroutines.CoroutineContext

class CollectionViewHolder(
    val view: View,
    val actionEmitter: Observer<SessionControlAction>,
    val job: Job,
    override val containerView: View? = view
) :
    RecyclerView.ViewHolder(view), LayoutContainer, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private lateinit var collection: TabCollection
    private var state = CollectionState.Collapsed
    private var collectionMenu: CollectionItemMenu

    init {
        collectionMenu = CollectionItemMenu(view.context) {
            when (it) {
                is CollectionItemMenu.Item.DeleteCollection -> actionEmitter.onNext(CollectionAction.Delete(collection))
                is CollectionItemMenu.Item.AddTab -> actionEmitter.onNext(CollectionAction.AddTab(collection))
                is CollectionItemMenu.Item.RenameCollection -> actionEmitter.onNext(CollectionAction.Rename(collection))
                is CollectionItemMenu.Item.OpenTabs -> actionEmitter.onNext(CollectionAction.OpenTabs(collection))
            }
        }

        collection_overflow_button.run {
            increaseTapArea(buttonIncreaseDps)
            setOnClickListener {
                collectionMenu.menuBuilder
                    .build(view.context)
                    .show(anchor = it, orientation = BrowserMenu.Orientation.DOWN)
            }
        }

        collection_share_button.run {
            increaseTapArea(buttonIncreaseDps)
            setOnClickListener {
                actionEmitter.onNext(CollectionAction.ShareTabs(collection))
            }
        }

        view.setOnClickListener {
            updateState()
        }

        view.collection_icon.setColorFilter(ContextCompat.getColor(
            view.context,
            getNextIconColor()),
            android.graphics.PorterDuff.Mode.SRC_IN
        )
    }

    fun bindSession(collection: TabCollection) {
        this.collection = collection
        updateCollectionUI()
    }

    private fun updateCollectionUI() {
        view.collection_title.text = collection.title

        var hostNameList = listOf<String>()

        collection.tabs.forEach {
            hostNameList += it.hostname.capitalize()
        }

        var tabsDisplayed = 0
        val titleList = hostNameList.joinToString(", ") {
            if (it.length > maxTitleLength) {
                it.substring(0,
                    maxTitleLength
                ) + "..."
            } else {
                tabsDisplayed += 1
                it
            }
        }

        view.collection_description.text = titleList

        if (collection.expanded) {
            (view.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = 0
            collection_title.setPadding(0, 0, 0, EXPANDED_PADDING)
            view.background = ContextCompat.getDrawable(view.context, R.drawable.rounded_top_corners)
            view.collection_description.visibility = View.GONE
            view.expand_button.setImageDrawable(ContextCompat.getDrawable(view.context, R.drawable.ic_chevron_up))
        } else {
            (view.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = COLLAPSED_MARGIN
            view.background = ContextCompat.getDrawable(view.context, R.drawable.rounded_all_corners)
            view.collection_description.visibility = View.VISIBLE
            view.expand_button.setImageDrawable(ContextCompat.getDrawable(view.context, R.drawable.ic_chevron_down))
        }
    }

    private fun updateState() {
        state = when (state) {
            CollectionState.Expanded -> {
                actionEmitter.onNext(CollectionAction.Collapse(collection))
                CollectionState.Collapsed
            }
            CollectionState.Collapsed -> {
                actionEmitter.onNext(CollectionAction.Expand(collection))
                CollectionState.Expanded
            }
        }
    }

    @Suppress("ComplexMethod", "MagicNumber")
    private fun getNextIconColor(): Int {
        with(view.context) {
            var sessionColorIndex = Settings.getInstance(this).preferences
                .getInt(getString(R.string.pref_key_collection_color), 0)

            val iconResource = when (sessionColorIndex) {
                0 -> R.color.collection_icon_color_violet
                1 -> R.color.collection_icon_color_blue
                2 -> R.color.collection_icon_color_pink
                3 -> R.color.collection_icon_color_green
                4 -> R.color.collection_icon_color_yellow
                else -> R.color.white_color
            }

            if (sessionColorIndex >= MAX_COLOR_INDEX) { sessionColorIndex = 0 } else { sessionColorIndex += 1 }

            Settings.getInstance(this).preferences.edit()
                .putInt(getString(R.string.pref_key_collection_color), sessionColorIndex).apply()

            return iconResource
        }
    }

    companion object {
        const val MAX_COLOR_INDEX = 4
        const val EXPANDED_PADDING = 60
        const val COLLAPSED_MARGIN = 12
        const val LAYOUT_ID = R.layout.collection_home_list_row
        const val maxTitleLength = 20
        const val buttonIncreaseDps = 24
    }

    enum class CollectionState {
        Expanded, Collapsed
    }
}

class CollectionItemMenu(
    private val context: Context,
    private val onItemTapped: (Item) -> Unit = {}
) {
    sealed class Item {
        object DeleteCollection : Item()
        object AddTab : Item()
        object RenameCollection : Item()
        object OpenTabs : Item()
    }

    val menuBuilder by lazy { BrowserMenuBuilder(menuItems) }

    private val menuItems by lazy {
        listOf(
            SimpleBrowserMenuItem(
                context.getString(R.string.collection_delete),
                textColorResource = DefaultThemeManager.resolveAttribute(R.attr.destructive, context)
            ) {
                onItemTapped.invoke(Item.DeleteCollection)
            },
            SimpleBrowserMenuItem(
                context.getString(R.string.add_tab)
            ) {
                onItemTapped.invoke(Item.AddTab)
            },
            SimpleBrowserMenuItem(
                context.getString(R.string.collection_rename)
            ) {
                onItemTapped.invoke(Item.RenameCollection)
            },
            SimpleBrowserMenuItem(
                context.getString(R.string.collection_open_tabs)
            ) {
                onItemTapped.invoke(Item.OpenTabs)
            }
        )
    }
}
