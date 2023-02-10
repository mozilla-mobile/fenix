package org.mozilla.fenix.utils

import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout

/**
 * This class is a workaround for the issue described in [https://bugzilla.mozilla.org/show_bug.cgi?id=1812144]
 * It is used to prevent the app bar from collapsing when the recycler view is empty.
 */
@Suppress("MaxLineLength")
class AppBarLayoutBehaviorEmptyRecyclerView : AppBarLayout.Behavior() {
    private var isRecyclerViewScrollable = false

    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: AppBarLayout, ev: MotionEvent): Boolean {
        return isRecyclerViewScrollable && super.onInterceptTouchEvent(parent, child, ev)
    }

    override fun onStartNestedScroll(
        parent: CoordinatorLayout,
        child: AppBarLayout,
        directTargetChild: View,
        target: View,
        nestedScrollAxes: Int,
        type: Int,
    ): Boolean {
        updateScrollableState(target)
        return isRecyclerViewScrollable && super.onStartNestedScroll(parent, child, directTargetChild, target, nestedScrollAxes, type)
    }

    override fun onNestedFling(
        coordinatorLayout: CoordinatorLayout,
        child: AppBarLayout,
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean,
    ): Boolean {
        updateScrollableState(target)
        return isRecyclerViewScrollable && super.onNestedFling(coordinatorLayout, child, target, velocityX, velocityY, consumed)
    }

    /**
     * If the child is a [RecyclerView], check if it is scrollable. Otherwise, assume it is scrollable.
     * This is a workaround because the RecyclerView is having itemCount 2 when all items are disabled,
     * so we are checking that instead 0 count.
     */
    private fun updateScrollableState(child: View) {
        isRecyclerViewScrollable = if (child is RecyclerView) {
            val rvAdapter = child.adapter
            rvAdapter != null && rvAdapter.itemCount > 2
        } else {
            true
        }
    }
}
