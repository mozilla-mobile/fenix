package org.mozilla.fenix.uicomponents

import android.content.Context
import android.util.AttributeSet
import android.util.Log
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
): FrameLayout(context, attrs) {
    var style : ButtonStyle = ButtonStyle.NEUTRAL

    var textString: String? = null
    var drawableRes: Int? = null

    init {
        View.inflate(
            context,
            R.layout.mozilla_button,
            this
        )

        context.withStyledAttributes(attrs, R.styleable.MozillaButton, 0, 0) {
            val styleId = getInt(
                R.styleable.MozillaButton_moz_button_type,
                0
            )

            textString = getString(
                R.styleable.MozillaButton_moz_button_text
            )

            drawableRes = getResourceId(
                R.styleable.MozillaButton_moz_button_image,
                0
            )

            setStyle(styleId)
        }

        mozilla_button.increaseTapArea(12)

        mozilla_button.setOnClickListener {
            Log.d("Sawyer", "buttonClick")
        }
    }


        //View.inflate(context, R.layout.mozilla_button, this)

    // TODO: How to better map this enum?
    private fun setStyle(id: Int) {
        Log.d("Sawyer", "setting style: $id")
        style = when (id) {
            0 -> ButtonStyle.NEUTRAL
            1 -> ButtonStyle.POSITIVE
            2 -> ButtonStyle.DESTRUCTIVE
            else -> ButtonStyle.NEUTRAL
        }

        updateButtonStyles()

        // Set certain layouts based on that
    }

    private fun updateButtonStyles() {
        when (style) {
            ButtonStyle.NEUTRAL -> {
                mozilla_button.backgroundTintList =  ContextCompat.getColorStateList(
                    context,
                    R.color.grey_button_color
                )
            }
            ButtonStyle.POSITIVE -> {
                mozilla_button.backgroundTintList =  ContextCompat.getColorStateList(
                    context,
                    ThemeManager.resolveAttribute(R.attr.accent, context)
                )
            }
            ButtonStyle.DESTRUCTIVE -> {
                mozilla_button.backgroundTintList =  ContextCompat.getColorStateList(
                    context,
                    R.color.grey_button_color
                )
            }
        }

        mozilla_button.text = textString

        mozilla_button.icon = (ContextCompat.getDrawable(context, drawableRes!!))
    }


    enum class ButtonStyle {
        NEUTRAL,
        POSITIVE,
        DESTRUCTIVE
    }
}