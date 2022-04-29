/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.search.toolbar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import mozilla.components.browser.state.search.SearchEngine
import mozilla.components.concept.toolbar.Toolbar
import mozilla.components.lib.state.ext.flowScoped
import mozilla.components.support.ktx.android.content.res.resolveAttribute
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged
import org.mozilla.fenix.R
import org.mozilla.fenix.search.SearchDialogFragmentStore
import java.lang.ref.WeakReference

/**
 * A [Toolbar.Action] implementation that shows a [SearchSelector].
 *
 * @property store [SearchDialogFragmentStore] containing the complete state of the search dialog.
 * @property menu An instance of [SearchSelectorMenu] to display a popup menu for the search
 * selections.
 * @property viewLifecycleOwner [LifecycleOwner] life cycle owner for the view.
 */
class SearchSelectorToolbarAction(
    private val store: SearchDialogFragmentStore,
    private val menu: SearchSelectorMenu,
    private val viewLifecycleOwner: LifecycleOwner
) : Toolbar.Action {

    private var reference = WeakReference<SearchSelector>(null)

    override fun createView(parent: ViewGroup): View {
        val context = parent.context

        store.flowScoped(viewLifecycleOwner) { flow ->
            flow.map { state -> state.searchEngineSource.searchEngine }
                .ifChanged()
                .collect { searchEngine ->
                    searchEngine?.let {
                        updateIcon(context, it)
                    }
                }
        }

        return SearchSelector(context).apply {
            reference = WeakReference(this)

            setOnClickListener {
                menu.menuController.show(anchor = it)
            }

            setBackgroundResource(
                context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless)
            )
        }
    }

    override fun bind(view: View) = Unit

    private fun updateIcon(context: Context, searchEngine: SearchEngine) {
        val iconSize =
            context.resources.getDimensionPixelSize(R.dimen.preference_icon_drawable_size)
        val scaledIcon = Bitmap.createScaledBitmap(
            searchEngine.icon,
            iconSize,
            iconSize,
            true
        )
        val icon = BitmapDrawable(context.resources, scaledIcon)

        reference.get()?.setIcon(icon, searchEngine.name)
    }
}
