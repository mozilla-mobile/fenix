/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.uicomponents

import android.content.Context
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import kotlinx.android.synthetic.main.mozilla_button.view.*
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.increaseTapArea
import org.mozilla.fenix.theme.ThemeManager

class MozillaButton(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    private var style: ButtonStyle = ButtonStyle.NEUTRAL
    private var textString = ""
    private var drawableRes = -1

    init {
        View.inflate(context, R.layout.mozilla_button, this)

        context.withStyledAttributes(attrs, R.styleable.MozillaButton, 0, 0) {
            style = valueOf(getInt(R.styleable.MozillaButton_moz_button_type, -1))
            textString = getString(R.styleable.MozillaButton_moz_button_text) ?: ""
            drawableRes = getResourceId(R.styleable.MozillaButton_moz_button_image, -1)
        }

        mozilla_button.increaseTapArea(TAP_AREA_INCREASE)

        updateButtonStyles()
    }

    private fun updateButtonStyles() {
        if (drawableRes != -1) {
            mozilla_button.icon = (ContextCompat.getDrawable(context, drawableRes))
        }

        when (style) {
            ButtonStyle.NEUTRAL -> {
                mozilla_button.icon?.setColorFilter(
                    ContextCompat.getColor(context, R.color.button_text_color), PorterDuff.Mode.SRC_IN)
                mozilla_button.setTextColor(ContextCompat.getColor(context, R.color.button_text_color))
                mozilla_button.backgroundTintList = ContextCompat.getColorStateList(
                    context,
                    R.color.grey_button_color
                )
            }
            ButtonStyle.POSITIVE -> {
                mozilla_button.icon?.setColorFilter(
                    ContextCompat.getColor(context,
                        ThemeManager.resolveAttribute(R.attr.contrastText, context)
                ), PorterDuff.Mode.SRC_IN)

                mozilla_button.setTextColor(ContextCompat.getColor(
                    context,
                    ThemeManager.resolveAttribute(R.attr.contrastText, context))
                )
                mozilla_button.backgroundTintList = ContextCompat.getColorStateList(
                    context,
                    ThemeManager.resolveAttribute(R.attr.accent, context)
                )
            }
            ButtonStyle.DESTRUCTIVE -> {
                mozilla_button.icon?.setColorFilter(
                    ContextCompat.getColor(context, R.color.destructive_button_text_color), PorterDuff.Mode.SRC_IN)
                mozilla_button.setTextColor(ContextCompat.getColor(context, R.color.destructive_button_text_color))
                mozilla_button.backgroundTintList = ContextCompat.getColorStateList(
                    context,
                    R.color.grey_button_color
                )
            }
        }

        mozilla_button.text = textString
    }

    private fun valueOf(value: Int): ButtonStyle = ButtonStyle.values().first { it.styleId == value }

    enum class ButtonStyle(var styleId: Int) {
        NEUTRAL(0),
        POSITIVE(1),
        DESTRUCTIVE(2)
    }

    companion object {
        private const val TAP_AREA_INCREASE = 12
    }
}
