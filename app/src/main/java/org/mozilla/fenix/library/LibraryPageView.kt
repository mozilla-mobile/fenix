/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library

import android.content.Context
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.asActivity
import org.mozilla.fenix.ext.getColorFromAttr
import org.mozilla.fenix.ext.setToolbarColors

open class LibraryPageView(
    override val containerView: ViewGroup
) : LayoutContainer {
    protected val context: Context inline get() = containerView.context
    protected val activity = context.asActivity()

    protected fun setUiForNormalMode(
        title: String?,
        libraryItemsList: RecyclerView
    ) {
        updateToolbar(
            title = title,
            foregroundColor = context.getColorFromAttr(R.attr.primaryText),
            backgroundColor = context.getColorFromAttr(R.attr.foundation)
        )
        libraryItemsList.adapter?.notifyDataSetChanged()
    }

    protected fun setUiForSelectingMode(
        title: String?,
        libraryItemsList: RecyclerView
    ) {
        updateToolbar(
            title = title,
            foregroundColor = ContextCompat.getColor(context, R.color.white_color),
            backgroundColor = context.getColorFromAttr(R.attr.accentHighContrast)
        )
        libraryItemsList.adapter?.notifyDataSetChanged()
    }

    private fun updateToolbar(title: String?, foregroundColor: Int, backgroundColor: Int) {
        activity?.title = title
        val toolbar = activity?.findViewById<Toolbar>(R.id.navigationToolbar)
        toolbar?.setToolbarColors(foregroundColor, backgroundColor)
    }
}
