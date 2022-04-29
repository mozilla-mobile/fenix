/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.browser

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_IDLE
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.tabstray.TabTouchCallback
import mozilla.components.browser.tabstray.TabsTray
import mozilla.components.support.ktx.android.content.getColorFromAttr
import mozilla.components.support.ktx.android.content.getDrawableWithTint
import mozilla.components.support.ktx.android.util.dpToPx
import org.mozilla.fenix.R
import org.mozilla.fenix.home.sessioncontrol.SwipeToDeleteCallback

/**
 * A callback for consumers to know when a [RecyclerView.ViewHolder] is about to be touched.
 * Return false if the custom behaviour should be ignored.
 */
typealias OnViewHolderTouched = (RecyclerView.ViewHolder) -> Boolean

/**
 * A callback for consumers to know when a [RecyclerView.ViewHolder] is about to be drawn.
 * Return false if the custom drawing should be ignored.
 */
typealias OnViewHolderToDraw = (RecyclerView.ViewHolder) -> Boolean

/**
 * An [ItemTouchHelper] for handling tab swiping to delete.
 *
 * @param onViewHolderTouched See [OnViewHolderTouched].
 */
class TabsTouchHelper(
    interactionDelegate: TabsTray.Delegate,
    onViewHolderTouched: OnViewHolderTouched = { true },
    onViewHolderDraw: OnViewHolderToDraw = { true },
    featureNameHolder: FeatureNameHolder,
    delegate: Callback = TouchCallback(interactionDelegate, onViewHolderTouched, onViewHolderDraw, featureNameHolder),
) : ItemTouchHelper(delegate)

/**
 * An [ItemTouchHelper.Callback] for drawing custom layouts on [RecyclerView.ViewHolder] interactions.
 *
 * @param onViewHolderTouched invoked when a tab is about to be swiped. See [OnViewHolderTouched].
 */
class TouchCallback(
    delegate: TabsTray.Delegate,
    private val onViewHolderTouched: OnViewHolderTouched,
    private val onViewHolderDraw: OnViewHolderToDraw,
    featureNameHolder: FeatureNameHolder,
    onRemove: (TabSessionState) -> Unit = { delegate.onTabClosed(it, featureNameHolder.featureName) }
) : TabTouchCallback(onRemove) {

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        if (!onViewHolderTouched.invoke(viewHolder)) {
            return ItemTouchHelper.Callback.makeFlag(ACTION_STATE_IDLE, 0)
        }

        return super.getMovementFlags(recyclerView, viewHolder)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

        if (!onViewHolderDraw.invoke(viewHolder)) {
            return
        }

        val icon = recyclerView.context.getDrawableWithTint(
            R.drawable.ic_delete,
            recyclerView.context.getColorFromAttr(R.attr.textWarning)
        )!!
        val background = AppCompatResources.getDrawable(
            recyclerView.context,
            R.drawable.swipe_delete_background
        )!!
        val itemView = viewHolder.itemView
        val iconLeft: Int
        val iconRight: Int
        val margin =
            SwipeToDeleteCallback.MARGIN.dpToPx(recyclerView.resources.displayMetrics)
        val iconWidth = icon.intrinsicWidth
        val iconHeight = icon.intrinsicHeight
        val cellHeight = itemView.bottom - itemView.top
        val iconTop = itemView.top + (cellHeight - iconHeight) / 2
        val iconBottom = iconTop + iconHeight

        when {
            dX > 0 -> { // Swiping to the right
                iconLeft = itemView.left + margin
                iconRight = itemView.left + margin + iconWidth
                background.setBounds(
                    itemView.left, itemView.top,
                    (itemView.left + dX).toInt() + SwipeToDeleteCallback.BACKGROUND_CORNER_OFFSET,
                    itemView.bottom
                )
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                draw(background, icon, c)
            }
            dX < 0 -> { // Swiping to the left
                iconLeft = itemView.right - margin - iconWidth
                iconRight = itemView.right - margin
                background.setBounds(
                    (itemView.right + dX).toInt() - SwipeToDeleteCallback.BACKGROUND_CORNER_OFFSET,
                    itemView.top, itemView.right, itemView.bottom
                )
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                draw(background, icon, c)
            }
            else -> { // View not swiped
                background.setBounds(0, 0, 0, 0)
                icon.setBounds(0, 0, 0, 0)
            }
        }
    }

    private fun draw(
        background: Drawable,
        icon: Drawable,
        c: Canvas
    ) {
        background.draw(c)
        icon.draw(c)
    }
}
