/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.core.view.GestureDetectorCompat

/**
 * Interface that allows intercepting and handling swipe gestures received in a [SwipeGestureLayout].
 */
interface SwipeGestureListener {

    /**
     * Called when the [SwipeGestureLayout] detects the start of a swipe gesture. The listener
     * should return true if it wants to handle the swipe gesture. If the listener returns false
     * it will not receive any callbacks for future events that the swipe produces.
     *
     * @param start the initial point where the gesture started
     * @param next the next point in the gesture
     */
    fun onSwipeStarted(start: PointF, next: PointF): Boolean

    /**
     * Called when the swipe gesture receives a new event.
     *
     * @param distanceX the change along the x-axis since the last swipe update
     * @param distanceY the change along the y-axis since the last swipe update
     */
    fun onSwipeUpdate(distanceX: Float, distanceY: Float)

    /**
     * Called when the user finishes the swipe gesture (ie lifts their finger off the screen)
     *
     * @param velocityX the velocity of the swipe along the x-axis
     * @param velocityY the velocity of the swipe along the y-axis
     */
    fun onSwipeFinished(velocityX: Float, velocityY: Float)
}

/**
 * A [FrameLayout] that allows listeners to intercept and handle swipe events.
 *
 * Listeners are called in the order they are added and the first listener to intercept a swipe event
 * is the only listener that will receive events for the duration of that swipe.
 */
class SwipeGestureLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent?): Boolean {
            return true
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent?,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            val start = e1?.let { event -> PointF(event.rawX, event.rawY) } ?: return false
            val next = e2?.let { event -> PointF(event.rawX, event.rawY) } ?: return false

            if (activeListener == null && !handledInitialScroll) {
                activeListener = listeners.firstOrNull { listener ->
                    listener.onSwipeStarted(start, next)
                }
                handledInitialScroll = true
            }
            activeListener?.onSwipeUpdate(distanceX, distanceY)
            return activeListener != null
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent?,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            activeListener?.onSwipeFinished(velocityX, velocityY)
            return if (activeListener != null) {
                activeListener = null
                true
            } else {
                false
            }
        }
    }

    private val gestureDetector = GestureDetectorCompat(context, gestureListener)

    private val listeners = mutableListOf<SwipeGestureListener>()
    private var activeListener: SwipeGestureListener? = null
    private var handledInitialScroll = false

    fun addGestureListener(listener: SwipeGestureListener) {
        listeners.add(listener)
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        return when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                handledInitialScroll = false
                gestureDetector.onTouchEvent(event)
                false
            }
            else -> gestureDetector.onTouchEvent(event)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return when (event?.actionMasked) {
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                gestureDetector.onTouchEvent(event)
                // If the active listener is not null here, then we haven't detected a fling
                // so notify the listener that the swipe was finished with 0 velocity
                activeListener?.onSwipeFinished(
                    velocityX = 0f,
                    velocityY = 0f
                )
                activeListener = null
                false
            }
            else -> gestureDetector.onTouchEvent(event)
        }
    }
}
