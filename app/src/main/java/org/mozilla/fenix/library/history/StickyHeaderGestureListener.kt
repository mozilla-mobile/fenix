package org.mozilla.fenix.library.history

import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

class StickyHeaderGestureListener(
    private val stickyHeaderHeight: () -> Float,
    private val recyclerView: RecyclerView,
    private val onStickyHeaderClicked: (Int) -> Unit
) : GestureDetector.SimpleOnGestureListener() {
    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        val height = stickyHeaderHeight.invoke()
        if (e.y < height) {
            val itemAdapterPosition = recyclerView.findChildViewUnder(0f, height)?.let {
                recyclerView.layoutManager?.getPosition(it)
            }?.also {
                onStickyHeaderClicked.invoke(it)
            }
            val childView = recyclerView.findChildViewUnder(0f, height)
//            val itemAdapterPosition2 = recyclerView.layoutManager?.getPosition(childView!!)
            Log.d("kolobok", "StickyHeaderGestureListener.onSingleTapConfirmed, it works! itemAdapterPosition = $itemAdapterPosition")
            //Do stuff here no you have the position of the item that's been clicked
            return true
        }
        Log.d("kolobok", "StickyHeaderGestureListener.onSingleTapConfirmed, it doesn't work!")
        return super.onSingleTapConfirmed(e)
    }

    override fun onDown(e: MotionEvent): Boolean {
        val touchY = e.y
        val height = stickyHeaderHeight.invoke()
        return if (touchY < height) {
            Log.d("kolobok", "StickyHeaderGestureListener.onDown, it works!")
            true
        } else {
            Log.d("kolobok", "StickyHeaderGestureListener.onDown, it doesn't work!")
            super.onDown(e)
        }
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        val touchY = e.y
        val height = stickyHeaderHeight.invoke()
        return if (touchY < height) {
            Log.d("kolobok", "StickyHeaderGestureListener.onSingleTapUp, it works!")
            true
        } else {
            Log.d("kolobok", "StickyHeaderGestureListener.onSingleTapUp, it doesn't work!")
            super.onSingleTapUp(e)
        }
    }

}
