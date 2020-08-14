/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_UP
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.graphics.BlendModeColorFilterCompat.createBlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat.SRC_IN
import mozilla.components.support.ktx.android.content.getColorFromAttr
import mozilla.components.support.ktx.android.view.putCompoundDrawablesRelativeWithIntrinsicBounds
import org.mozilla.fenix.R

/**
 * An [AppCompatEditText] that shows a clear button to the user.
 */
class ClearableEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.editTextStyle
) :
    AppCompatEditText(context, attrs, defStyleAttr) {

    /**
     * Clears the text when the clear icon is touched.
     *
     * Since the icon is just a compound drawable, we check the tap location
     * to see if the X position of the tap is where the drawable is located.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (shouldShowClearButton(length()) && event.action == ACTION_UP && event.endDrawableTouched()) {
            setText("")
            return true
        }
        return super.onTouchEvent(event)
    }

    /**
     * Displays a clear icon if text has been entered.
     */
    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        // lengthAfter has inconsistent behaviour when there are spaces in the entered text, so we'll use text.length.
        val textLength = text?.length ?: 0
        val drawable = if (shouldShowClearButton(textLength)) {
            AppCompatResources.getDrawable(context, R.drawable.ic_clear)?.apply {
                colorFilter = createBlendModeColorFilterCompat(context.getColorFromAttr(R.attr.primaryText), SRC_IN)
            }
        } else {
            null
        }
        putCompoundDrawablesRelativeWithIntrinsicBounds(end = drawable)
    }

    /**
     * Checks if the clear button should be displayed.
     *
     * The button should be displayed if the user has entered valid text.
     * @param length Length of the text the user has entered.
     */
    private fun shouldShowClearButton(length: Int) =
        length > 0 && error == null

    /**
     * Returns true if the location of the [MotionEvent] is on top of the end drawable.
     */
    private fun MotionEvent.endDrawableTouched() =
        (layoutDirection == LAYOUT_DIRECTION_LTR && rawX >= (right - compoundPaddingRight)) ||
        (layoutDirection == LAYOUT_DIRECTION_RTL && rawX <= (left + compoundPaddingLeft))
}
