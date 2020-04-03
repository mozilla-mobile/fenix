package org.mozilla.fenix.uicomponents

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.core.content.withStyledAttributes
import com.google.android.material.button.MaterialButton
import org.mozilla.fenix.R

class MozillaButton(
    context: Context,
    attrs: AttributeSet? = null
): MaterialButton(context, attrs) {
    var style : ButtonStyle = ButtonStyle.NEUTRAL

    init {

        Log.d("Sawyer", "init: $attrs")

        context.withStyledAttributes(attrs, R.styleable.MozillaButton, R.style.Widget_MaterialComponents_Button_TextButton, 0) {
            val styleId = getInt(
                R.styleable.MozillaButton_type,
                0
            )

            setStyle(styleId)
        }
    }

        //View.inflate(context, R.layout.mozilla_button, this)

    private fun setStyle(id: Int) {
        Log.d("Sawyer", "setting style: $id")
        style = when (id) {
            0 -> ButtonStyle.NEUTRAL
            1 -> ButtonStyle.POSITIVE
            2 -> ButtonStyle.DESTRUCTIVE
            else -> ButtonStyle.NEUTRAL
        }

        // Set certain layouts based on that
    }


    enum class ButtonStyle {
        NEUTRAL,
        POSITIVE,
        DESTRUCTIVE
    }
}