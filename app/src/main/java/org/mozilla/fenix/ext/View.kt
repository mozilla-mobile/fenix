/* This Source Code Form is subject to the terms of the Mozilla Public
   License, v. 2.0. If a copy of the MPL was not distributed with this
   file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import android.graphics.Rect
import android.view.TouchDelegate
import android.view.View
import mozilla.components.browser.menu.BrowserMenu
import mozilla.components.support.ktx.android.content.res.pxToDp

fun View?.increaseTapArea(extraDps: Int) {
    val dips = this!!.resources.pxToDp(extraDps)
    val parent = this.parent as View
    parent.post {
        val touchRect = Rect()
        getHitRect(touchRect)
        touchRect.top -= dips
        touchRect.left -= dips
        touchRect.right += dips
        touchRect.bottom += dips
        parent.touchDelegate = TouchDelegate(touchRect, this)
    }
}

fun View.getMenuDirectionForLocation(): BrowserMenu.Orientation {
    val location = IntArray(2)
    getLocationInWindow(location)
    return if (location[1] > (rootView.measuredHeight / 2))
        BrowserMenu.Orientation.UP else
        BrowserMenu.Orientation.DOWN
}
