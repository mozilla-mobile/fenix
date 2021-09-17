/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.browser

import android.content.Context
import android.util.AttributeSet
import android.view.DragEvent
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.concept.tabstray.Tab
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

    // Find the closest item to the x/y position.
    // Then, based on the offset between the first and second
    // determine if it goes before or after that item.
    // This will fail if there's a spiral layout or something
    // but should work perfectly for lists and grids.
    private fun getDropPosition(x: Float, y: Float): Pair<Int, Boolean>? {
        if (childCount < 2) return null // If there's 0 or 1 tabs visible, can't reorder
        val first = getChildAt(0)!!
        val second = getChildAt(1)!!
        val xOffset = second.x - first.x
        val yOffset = second.y - first.y

        var bestDist = Float.MAX_VALUE
        var bestPos = 0
        var placeAfter = false
        for (i in 0 until childCount) {
            val proposedTarget = getChildAt(i)!!
            val xDiff = x - (proposedTarget.x + proposedTarget.width / 2)
            val yDiff = y - (proposedTarget.y + proposedTarget.height / 2)
            val dist = abs(xDiff) + abs(yDiff)
            if (dist < bestDist) {
                bestDist = dist
                bestPos = getChildAdapterPosition(proposedTarget)
                val modifier = (xDiff * xOffset) + (yDiff * yOffset)
                placeAfter = (modifier > 0)
            }
        }
        return Pair(bestPos, placeAfter)
    }
    private val dragListen = OnDragListener { _, event ->
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                // This check is required for the unchecked cast later on
                if (event.localState is Collection<*>) {
                    (event.localState as Collection<*>).all { it is Tab }
                } else false
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                true
            }
            DragEvent.ACTION_DRAG_LOCATION -> {
                true
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                true
            }
            DragEvent.ACTION_DROP -> {
                val target = getDropPosition(event.x, event.y)
                if (target != null) {
                    val (targetPos, placeAfter) = target
                    val filter = tabsFeature.defaultTabsFilter
                    @Suppress("UNCHECKED_CAST") // Cast is checked on drag start
                    interactor.onTabsMove(event.localState as Collection<Tab>, targetPos, placeAfter, filter)
                }
                true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                true
            }
            else -> { // Unknown action
                false
            }
        }
    }
}
