/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.browser

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.DragEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.tabstray.TabViewHolder
import mozilla.components.feature.tabs.tabstray.TabsFeature
import org.mozilla.fenix.ext.components
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

    /**
     * A [TabsFeature] is required for each browser list to ensure one always exists for displaying
     * tabs.
     */
    abstract val tabsFeature: TabsFeature

    // NB: The use cases here are duplicated because there isn't a nicer
    // way to share them without a better dependency injection solution.
    protected val selectTabUseCase = SelectTabUseCaseWrapper(
        context.components.analytics.metrics,
        context.components.useCases.tabsUseCases.selectTab
    ) {
        interactor.onBrowserTabSelected()
    }

    protected val removeTabUseCase = RemoveTabUseCaseWrapper(
        context.components.analytics.metrics
    ) { sessionId ->
        interactor.onDeleteTab(sessionId)
    }

    protected val swipeToDelete by lazy {
        SwipeToDeleteBinding(tabsTrayStore)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        swipeToDelete.start()

        adapter?.onAttachedToRecyclerView(this)
        this.setOnDragListener(dragListen)
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
    private fun findSourceView(id: String): View? {
        for (i in 0 until childCount) {
            val proposed = getChildAt(i)
            val targetHolder = findContainingViewHolder(proposed)
            if (targetHolder is TabViewHolder && targetHolder.tab?.id == id) {
                return proposed
            }
        }
        return null
    }
    private val dragListen = OnDragListener { _, event ->
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                if (event.localState is TabSessionState) {
                    val id = (event.localState as TabSessionState).id
                    val sourceView = findSourceView(id)
                    if (sourceView != null) {
                        sourceView.alpha = DRAG_TRANSPARENCY
                    }
                    true
                } else false
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                true
            }
            DragEvent.ACTION_DRAG_LOCATION -> {
                val source = (event.localState as TabSessionState)
                val target = getDropPosition(event.x, event.y, source.id)
                if (target != null) {
                    val (id, placeAfter) = target
                    interactor.onTabsMove(source.id, id, placeAfter)
                }
                true
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                true
            }
            DragEvent.ACTION_DROP -> {
                val source = (event.localState as TabSessionState)
                val target = getDropPosition(event.x, event.y, source.id)
                if (target != null) {
                    val (id, placeAfter) = target
                    interactor.onTabsMove(source.id, id, placeAfter)
                }
                true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                // Revert the invisibility
                val id = (event.localState as TabSessionState).id
                val sourceView = findSourceView(id)
                if (sourceView != null) {
                    sourceView.alpha = 1f
                }
                true
            }
            else -> { // Unknown action
                false
            }
        }
    }
    companion object {
        internal const val DRAG_TRANSPARENCY = 0.2f
    }
}
