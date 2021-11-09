/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.browser

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.util.AttributeSet
import android.view.DragEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.tabstray.TabViewHolder
import org.mozilla.fenix.tabstray.TabsTrayInteractor
import org.mozilla.fenix.tabstray.TabsTrayStore
import kotlin.math.abs

/**
 * The base class for a tabs tray list that wants to display browser tabs.
 */
abstract class AbstractBrowserTrayList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    lateinit var interactor: TabsTrayInteractor
    lateinit var tabsTrayStore: TabsTrayStore

    protected val swipeToDelete by lazy {
        SwipeToDeleteBinding(tabsTrayStore)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        swipeToDelete.start()

        adapter?.onAttachedToRecyclerView(this)
        this.setOnDragListener(dragListen)
        itemAnimator = DraggableItemAnimator()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        swipeToDelete.stop()

        // Notify the adapter that it is released from the view preemptively.
        adapter?.onDetachedFromRecyclerView(this)
        this.setOnDragListener(null)
    }

    // Find the closest item to the x/y position of the drop.
    private fun getDropPosition(x: Float, y: Float, source: String): Pair<String, Boolean>? {
        if (childCount < 2) return null // If there's 0 or 1 tabs visible, can't reorder
        var bestDist = Float.MAX_VALUE
        var bestOut: Pair<String, Boolean>? = null
        var seenSource = false
        for (i in 0 until childCount) {
            val proposedTarget = getChildAt(i)
            val targetHolder = findContainingViewHolder(proposedTarget)
            if (targetHolder is TabViewHolder) {
                var rect = Rect() // Get post-animation positioning
                getDecoratedBoundsWithMargins(proposedTarget, rect)
                val targetX = (rect.left + rect.right) / 2
                val targetY = (rect.top + rect.bottom) / 2
                val xDiff = x - targetX
                val yDiff = y - targetY
                val dist = abs(xDiff) + abs(yDiff)
                val id = targetHolder.tab?.id
                if (id == source) seenSource = true
                if (dist < bestDist && id != null) {
                    bestDist = dist
                    bestOut = Pair(id, seenSource)
                }
            }
        }
        return bestOut
    }
    private fun findSourceViewAndHolder(id: String): Pair<View, AbstractBrowserTabViewHolder>? {
        for (i in 0 until childCount) {
            val proposed = getChildAt(i)
            val targetHolder = findContainingViewHolder(proposed)
            if (targetHolder is AbstractBrowserTabViewHolder && targetHolder.tab?.id == id) {
                return Pair(proposed, targetHolder)
            }
        }
        return null
    }
    private val dragListen = OnDragListener { _, event ->
        if (event.localState is Pair<*, *>) {
            val (tab, dragOffset) = event.localState as Pair<*, *>
            if (tab is TabSessionState && dragOffset is PointF) {
                val sourceId = tab.id
                val sources = findSourceViewAndHolder(sourceId)

                when (event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> {
                        // Put the dragged tab on top of all other tabs
                        if (sources != null) {
                            val (sourceView, _) = sources
                            sourceView.elevation += DRAGGED_TAB_ELEVATION
                        }
                        true
                    }
                    DragEvent.ACTION_DRAG_ENTERED -> {
                        true
                    }
                    DragEvent.ACTION_DRAG_LOCATION -> {
                        val target = getDropPosition(event.x, event.y, tab.id)
                        if (target != null) {
                            val (targetId, placeAfter) = target
                            interactor.onTabsMove(tab.id, targetId, placeAfter)
                        }
                        // Move the tab's visual position
                        if (sources != null) {
                            val (sourceView, _) = sources
                            sourceView.x = event.x - dragOffset.x
                            sourceView.y = event.y - dragOffset.y
                        }

                        true
                    }
                    DragEvent.ACTION_DRAG_EXITED -> {
                        true
                    }
                    DragEvent.ACTION_DROP -> {
                        true
                    }
                    DragEvent.ACTION_DRAG_ENDED -> {
                        // Move tab to center, set dragging to false, return tab to normal height
                        if (sources != null) {
                            val (sourceView, sourceViewHolder) = sources
                            sourceView.elevation -= DRAGGED_TAB_ELEVATION
                            sourceView.animate()
                                .translationX(0f).translationY(0f)
                                .setDuration(itemAnimator?.moveDuration ?: 0)

                            sourceViewHolder.beingDragged = false
                        }
                        true
                    }
                    else -> { // Unknown action
                        false
                    }
                }
            } else false
        } else false
    }
    companion object {
        internal const val DRAGGED_TAB_ELEVATION = 10f
    }
}
