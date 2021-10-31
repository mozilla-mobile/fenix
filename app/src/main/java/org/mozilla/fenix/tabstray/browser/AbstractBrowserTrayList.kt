/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.browser

import android.content.Context
import android.util.AttributeSet
import android.view.DragEvent
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
    // This gets all the children of the tab tray, which will not include grouped tabs
    // Since those are children of the group which is a child of the tab tray.
    // Determine if the drop is before or after based on the current grid/list view setting
    private fun getDropPosition(x: Float, y: Float): Pair<String?, Boolean>? {
        if (childCount < 2) return null // If there's 0 or 1 tabs visible, can't reorder
        val isGrid = context.components.settings.gridTabView
        var bestDist = Float.MAX_VALUE
        var bestId: String? = null
        var placeAfter = false
        for (i in 0 until childCount) {
            val targetHolder = findViewHolderForAdapterPosition(i)
            val proposedTarget = getChildAt(i)!!
            val xDiff = x - (proposedTarget.x + proposedTarget.width / 2)
            val yDiff = y - (proposedTarget.y + proposedTarget.height / 2)
            val dist = abs(xDiff) + abs(yDiff)
            if (dist < bestDist && targetHolder is TabViewHolder) {
                val targetTabId = targetHolder.tab?.id
                bestDist = dist
                bestId = targetTabId
                val modifier = if (isGrid) xDiff else yDiff
                placeAfter = (modifier > 0)
            }
        }
        return Pair(bestId, placeAfter)
    }
    private val dragListen = OnDragListener { _, event ->
        when (event.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                // This check is required for the unchecked cast later on
                if (event.localState is Collection<*>) {
                    (event.localState as Collection<*>).all { it is TabSessionState }
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
                    val (targetId, placeAfter) = target
                    @Suppress("UNCHECKED_CAST") // Cast is checked on drag start
                    interactor.onTabsMove(event.localState as Collection<TabSessionState>, targetId, placeAfter)
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
