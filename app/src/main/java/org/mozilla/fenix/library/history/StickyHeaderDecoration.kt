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


class StickyHeaderDecoration(
    private val headerManager: HeaderManager
) : RecyclerView.ItemDecoration() {

    private var stickyHeaderHeight = 0
    private var stickyHeaderBottom = 0

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)

        // parent.getChildAt(0) won't work with collapsing correctly, because of how animation works.
        // When were are opening up a timegroup, the elements that have to be animated away from the
        // screen would be drawn over the recyclerview real items, and then animated away. To access
        // items that belong to the adapter, we have to get a view from the layoutManager.
        val topViewPosition = (parent.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        if (topViewPosition == RecyclerView.NO_POSITION) {
            return
        }

        val currentHeaderPosition: Int = headerManager.getHeaderPositionForItem(topViewPosition).also {
            if (it == -1) {
                // No sticky header drawn, click should be passed down to the recyclerview items.
                stickyHeaderBottom = 0
                return
            }
        }

        val stickyHeaderView = getHeaderView(currentHeaderPosition, parent, headerManager)
        fixLayoutSize(parent, stickyHeaderView)

        // It there is a view that collides with a sticky header, we should adjust the sticky header
        // position.
        getViewInContact(parent, stickyHeaderView.bottom, currentHeaderPosition, headerManager)?.let {
            if (headerManager.isHeader(parent.getChildAdapterPosition(it))) {
                val headerTopPosition = it.top - stickyHeaderView.height
                stickyHeaderBottom = headerTopPosition + stickyHeaderHeight
                moveHeader(c, stickyHeaderView, headerTopPosition.toFloat())
                return
            }
        }

        stickyHeaderBottom = stickyHeaderHeight
        drawHeader(c, stickyHeaderView)
    }

    fun getStickyHeaderBottom() : Float {
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
                if (headerManager.isHeader(parent.getChildAdapterPosition(view))) {
                    heightTolerance = stickyHeaderHeight - view.height
                }
            }

            //add heightTolerance if child top be in display area
            val childBottomPosition: Int = if (view.top > 0) {
                view.bottom + heightTolerance
            } else {
                view.bottom
            }
            if (childBottomPosition > contactPoint - 1) {
                if (view.top <= contactPoint) {
                    // This child overlaps the contactPoint
                    result = view
                    break
                }
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

interface HeaderManager {

    /**
     * This method gets called by [StickHeaderItemDecoration] to fetch the position of the header item in the adapter
     * that is used for (represents) item at specified position.
     * @param itemPosition int. Adapter's position of the item for which to do the search of the position of the header item.
     * @return int. Position of the header item in the adapter.
     */
    fun getHeaderPositionForItem(itemPosition: Int): Int

    /**
     * This method gets called by [StickHeaderItemDecoration] to get layout resource id for the header item at specified adapter's position.
     * @param headerPosition int. Position of the header item in the adapter.
     * @return int. Layout resource id.
     */
    fun getHeaderLayout(headerPosition: Int): Int

    /**
     * This method gets called by [StickHeaderItemDecoration] to setup the header View.
     * @param header View. Header to set the data on.
     * @param headerPosition int. Position of the header item in the adapter.
     */
    fun bindHeader(header: View, headerPosition: Int)

    /**
     * This method gets called by [StickHeaderItemDecoration] to verify whether the item represents a header.
     * @param itemPosition int.
     * @return true, if item at the specified adapter's position represents a header.
     */
    fun isHeader(itemPosition: Int): Boolean

    fun onStickyHeaderClicked(headerPosition: Int)
}
