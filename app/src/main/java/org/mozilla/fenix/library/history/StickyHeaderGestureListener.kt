/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import android.view.GestureDetector
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

class StickyHeaderGestureListener(
    private val recyclerView: RecyclerView,
    private val onStickyHeaderClicked: (Int) -> Unit,
    private val stickyHeaderBottom: () -> Float,
) : GestureDetector.SimpleOnGestureListener() {
    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        val bottom = stickyHeaderBottom.invoke()
        if (e.y < bottom) {
            recyclerView.findChildViewUnder(0f, e.y)?.let {
                recyclerView.layoutManager?.getPosition(it)
            }?.also {
                onStickyHeaderClicked.invoke(it)
            }
            return true
        }
        return super.onSingleTapConfirmed(e)
    }

    override fun onDown(e: MotionEvent): Boolean {
        val touchY = e.y
        val bottom = stickyHeaderBottom.invoke()
        return if (touchY < bottom) {
            true
        } else {
            super.onDown(e)
        }
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        val touchY = e.y
        val bottom = stickyHeaderBottom.invoke()
        return if (touchY < bottom) {
            true
        } else {
            super.onSingleTapUp(e)
        }
    }
}
