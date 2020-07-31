/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.gestures

import android.graphics.PointF
import android.view.View
import androidx.core.graphics.contains
import androidx.core.graphics.toPoint
import org.mozilla.fenix.ext.getRectWithScreenLocation
import org.mozilla.fenix.ext.getWindowInsets
import org.mozilla.fenix.ext.settings

/**
 * Checks if a point is within the bounds of a toolbar view. This accounts for the overlap
 * between the bottom toolbar and the system gesture area.
 */
fun PointF.isInToolbar(toolbar: View): Boolean {
    val toolbarLocation = toolbar.getRectWithScreenLocation()
    // In Android 10, the system gesture touch area overlaps the bottom of the toolbar, so
    // lets make our swipe area taller by that amount
    toolbar.getWindowInsets()?.let { insets ->
        if (toolbar.context.settings().shouldUseBottomToolbar) {
            toolbarLocation.top -= (insets.mandatorySystemGestureInsets.bottom - insets.stableInsetBottom)
        }
    }
    return toolbarLocation.contains(toPoint())
}
