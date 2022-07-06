/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec.makeMeasureSpec
import android.view.ViewGroup
import android.view.ViewGroup.getChildMeasureSpec
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * A decorator for a [RecyclerView] that makes header items in a list sticky. If a top visible item
 * in the list has a header somewhere up the list, a header view will be drawn on a [Canvas] over
 * the [RecyclerView]. It also adjusts the sticky header size if there is another header down the
 * list, that is in contact with the sticky header. The decorator can not capture click events.
 * [StickyHeaderGestureListener] is responsible for capturing and propagating clicks.
 */
class StickyHeaderDecoration(
    private val headerManager: HeaderManager
) : RecyclerView.ItemDecoration() {

    private var stickyHeaderHeight = 0
    private var stickyHeaderBottom = 0

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)

        // Getting the top element in the adapter or returning if there is a recalculation happening.
        val topViewPosition =
            (parent.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        if (topViewPosition == RecyclerView.NO_POSITION) {
            return
        }

        // Position of a header for a top viewHolder inside the adapter.
        val currentHeaderPosition: Int =
            headerManager.getHeaderPositionForItem(topViewPosition).also {
                if (it == -1) {
                    // No sticky header drawn, reset the sticky header bottom position, so that
                    // a click could be passed down to the recyclerview items.
                    stickyHeaderBottom = 0
                    return
                }
            }

        // Get the stickyHeader view.
        val stickyHeaderView = getHeaderView(currentHeaderPosition, parent, headerManager)
        fixLayoutSize(parent, stickyHeaderView)

        // It there is a view that collides with a sticky header, we should adjust the sticky header
        // position in relation to that view.
        getViewInContact(
            parent,
            stickyHeaderView.bottom,
            currentHeaderPosition,
            headerManager
        )?.let {
            val childPosition = parent.getChildAdapterPosition(it)
            if (childPosition != RecyclerView.NO_POSITION &&
                headerManager.isHeader(childPosition)
            ) {
                val headerTopPosition = it.top - stickyHeaderView.height
                stickyHeaderBottom = headerTopPosition + stickyHeaderHeight
                moveHeader(c, stickyHeaderView, headerTopPosition.toFloat())
                return
            }
        }

        // If there is no need to adjust its position, just draw the sticky header.
        stickyHeaderBottom = stickyHeaderHeight
        drawHeader(c, stickyHeaderView)
    }

    /**
     * Returns the bottom coordinate of the currently visible sticky header.
     * Based on the returned value, [StickyHeaderGestureListener] calculates if the click happened
     * over the sticky header or an actual item.
     */
    fun getStickyHeaderBottom(): Float {
        return stickyHeaderBottom.toFloat()
    }

    private fun getHeaderView(
        headerPosition: Int,
        parent: RecyclerView,
        headerManager: HeaderManager
    ): View {
        val layoutRes: Int = headerManager.getHeaderLayout(headerPosition)
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        headerManager.bindHeader(view, headerPosition)
        view.isClickable = true
        return view
    }

    private fun drawHeader(c: Canvas, header: View) {
        c.save()
        c.translate(0f, 0f)
        header.draw(c)
        c.restore()
    }

    private fun moveHeader(c: Canvas, stickyHeader: View, stickyHeaderHeight: Float) {
        c.save()
        c.translate(0f, stickyHeaderHeight)
        stickyHeader.draw(c)
        c.restore()
    }

    private fun getViewInContact(
        parent: RecyclerView,
        contactPoint: Int,
        stickyHeaderPosition: Int,
        headerManager: HeaderManager
    ): View? {
        var result: View? = null
        for (i in 0 until parent.childCount) {
            var heightTolerance = 0
            val view = parent.getChildAt(i)

            // measure height tolerance with child if child is another header
            if (stickyHeaderPosition != i) {
                val childPosition = parent.getChildAdapterPosition(view)
                if (childPosition != RecyclerView.NO_POSITION &&
                    headerManager.isHeader(childPosition)
                ) {
                    heightTolerance = stickyHeaderHeight - view.height
                }
            }

            // add heightTolerance if child top be in display area
            val childBottomPosition: Int = if (view.top > 0) {
                view.bottom + heightTolerance
            } else {
                view.bottom
            }
            val isOverlapping = childBottomPosition > contactPoint - 1 && view.top <= contactPoint
            if (isOverlapping) {
                result = view
                break
            }
        }
        return result
    }

    /**
     * Properly measures and layouts the top sticky header.
     * @param parent ViewGroup: RecyclerView in this case.
     */
    private fun fixLayoutSize(parent: ViewGroup, view: View) {
        // Specs for parent (RecyclerView)
        val parentWidth = makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
        val parentHeight = makeMeasureSpec(parent.height, View.MeasureSpec.UNSPECIFIED)

        // Specs for children (headers)
        val childWidth: Int = getChildMeasureSpec(
            parentWidth,
            parent.paddingLeft + parent.paddingRight,
            view.layoutParams.width
        )
        val childHeight: Int = getChildMeasureSpec(
            parentHeight,
            parent.paddingTop + parent.paddingBottom,
            view.layoutParams.height
        )
        view.measure(childWidth, childHeight)
        view.layout(
            0,
            0,
            view.measuredWidth,
            view.measuredHeight.also {
                stickyHeaderHeight = it
            }
        )
    }
}

/**
 * An interface for [HistoryAdapter] to provide information about header and populate it.
 */
interface HeaderManager {

    /**
     * This method gets called by [StickyHeaderDecoration] to verify whether the item represents a header.
     * @param itemPosition
     * @return Does the item at the specified adapter's position represent a header or not.
     */
    fun isHeader(itemPosition: Int): Boolean

    /**
     * This method gets called by [StickyHeaderDecoration] to fetch the position of the header item
     * in the adapter that is used for the item at the specified position.
     * @param itemPosition Adapter's position of the item for which to do the search of the position
     * of the header item.
     * @return Position of the header item in the adapter.
     */
    fun getHeaderPositionForItem(itemPosition: Int): Int

    /**
     * This method gets called by [StickyHeaderDecoration] to get layout resource id for the header
     * item at specified adapter's position.
     * @param headerPosition Position of the header item in the adapter.
     * @return Layout resource id.
     */
    fun getHeaderLayout(headerPosition: Int): Int

    /**
     * This method gets called by [StickyHeaderDecoration] to setup the header View.
     * @param header View to set the data on.
     * @param headerPosition Position of the header item in the adapter.
     */
    fun bindHeader(header: View, headerPosition: Int)
}
