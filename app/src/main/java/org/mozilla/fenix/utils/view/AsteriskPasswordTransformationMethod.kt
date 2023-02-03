package org.mozilla.fenix.utils.view

import android.text.method.PasswordTransformationMethod
import android.view.View

/**
 * A [PasswordTransformationMethod] that replaces all characters with asterisks.
 */
class AsteriskPasswordTransformationMethod : PasswordTransformationMethod() {
    override fun getTransformation(source: CharSequence, view: View): CharSequence {
        return PasswordCharSequence(source)
    }

    private class PasswordCharSequence(private val source: CharSequence) : CharSequence {
        override val length: Int
            get() = source.length

        override fun get(index: Int): Char {
            return '*'
        }

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
            return source.subSequence(startIndex, endIndex)
        }

        override fun toString(): String {
            return source.toString()
        }
    }
}
