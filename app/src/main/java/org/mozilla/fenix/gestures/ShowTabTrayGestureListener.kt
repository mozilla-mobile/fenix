/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.gestures

import android.app.Activity
import android.graphics.PointF
import android.view.View
import android.view.ViewConfiguration
import org.mozilla.fenix.components.toolbar.ToolbarPosition
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.isKeyboardVisible
import kotlin.math.abs

class ShowTabTrayGestureListener(
    private val activity: Activity,
    private val toolbarLayout: View,
    private val showTabTray: () -> Unit
) : SwipeGestureListener {

    private val touchSlop = ViewConfiguration.get(activity).scaledTouchSlop
    private val minimumFlingVelocity = ViewConfiguration.get(activity).scaledMinimumFlingVelocity

    override fun onSwipeStarted(start: PointF, next: PointF): Boolean {
        val dx = next.x - start.x
        // negative dy = swiping up
        val dy = next.y - start.y

        return activity.components.settings.toolbarPosition == ToolbarPosition.BOTTOM &&
            !toolbarLayout.isKeyboardVisible() && start.isInToolbar(toolbarLayout) &&
            -dy >= touchSlop && abs(dx) < abs(dy)
    }

    override fun onSwipeUpdate(distanceX: Float, distanceY: Float) {
        // Do nothing
    }

    override fun onSwipeFinished(velocityX: Float, velocityY: Float) {
        // Negative velocityY = swiping up
        if (-velocityY >= minimumFlingVelocity) {
            showTabTray.invoke()
        }
    }
}
