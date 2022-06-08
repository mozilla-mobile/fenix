package org.mozilla.fenix.library.history

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class StickyHeaderRecycler @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private val clicksFlow: MutableSharedFlow<ClickEvent> = MutableSharedFlow(
        extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    fun getClicksFlow() = clicksFlow as Flow<ClickEvent>

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent) : Boolean{
        if (e.action == MotionEvent.ACTION_DOWN) {
            for (i in 0 until itemDecorationCount) {
                val decor = getItemDecorationAt(i)
                if (decor is StickyHeaderDecoration && decor.isOnTouched(this, e)) {
                    clicksFlow.tryEmit(ClickEvent(e.x, e.y))
                    return true
                }
            }
        }
        return super.onTouchEvent(e)
    }

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_DOWN) {
            for (i in 0 until itemDecorationCount) {
                val decor = getItemDecorationAt(i)
                if (decor is StickyHeaderDecoration && decor.isOnTouched(this, e)) {
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(e)
    }
}

data class ClickEvent(val x: Float, val y: Float)