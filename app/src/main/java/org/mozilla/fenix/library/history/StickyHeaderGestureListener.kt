/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.history

import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

class StickyHeaderGestureListener(
    private val recyclerView: RecyclerView,
    private val onStickyHeaderClicked: (Int) -> Unit,
    private val stickyHeaderHeight: () -> Float,
) : GestureDetector.SimpleOnGestureListener() {
    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        val height = stickyHeaderHeight.invoke()
        if (e.y < height) {
            recyclerView.findChildViewUnder(0f, height)?.let {
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
        val height = stickyHeaderHeight.invoke()
        return if (touchY < height) {
            true
        } else {
            super.onDown(e)
        }
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        val touchY = e.y
        val height = stickyHeaderHeight.invoke()
        return if (touchY < height) {
            true
        } else {
            super.onSingleTapUp(e)
        }
    }
}
