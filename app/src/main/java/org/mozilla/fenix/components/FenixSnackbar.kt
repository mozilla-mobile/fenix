/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.ContentFrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.TextViewCompat
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.ContentViewCallback
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fenix_snackbar.view.*
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.increaseTapArea
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.utils.Mockable

@Mockable
class FenixSnackbar private constructor(
    parent: ViewGroup,
    content: View,
    contentViewCallback: FenixSnackbarCallback,
    isError: Boolean
) : BaseTransientBottomBar<FenixSnackbar>(parent, content, contentViewCallback) {

    init {
        view.background = null

        setAppropriateBackground(isError)

        content.snackbar_btn.increaseTapArea(actionButtonIncreaseDps)

        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
            content.snackbar_text,
            minTextSize,
            maxTextSize,
            stepGranularity,
            TypedValue.COMPLEX_UNIT_SP
        )
    }

    fun setAppropriateBackground(isError: Boolean) {
        view.snackbar_layout.background = if (isError) {
            AppCompatResources.getDrawable(context, R.drawable.fenix_snackbar_error_background)
        } else {
            AppCompatResources.getDrawable(context, R.drawable.fenix_snackbar_background)
        }
    }

    fun setText(text: String) = this.apply {
        view.snackbar_text.text = text
    }

    fun setLength(duration: Int) = this.apply {
        this.duration = duration
    }

    fun setAction(text: String, action: () -> Unit) = this.apply {
        view.snackbar_btn.apply {
            setText(text)
            visibility = View.VISIBLE
            setOnClickListener {
                action.invoke()
                dismiss()
            }
        }
    }

    companion object {
        const val LENGTH_LONG = Snackbar.LENGTH_LONG
        const val LENGTH_SHORT = Snackbar.LENGTH_SHORT
        const val LENGTH_ACCESSIBLE = 15000 /* 15 seconds in ms */
        const val LENGTH_INDEFINITE = Snackbar.LENGTH_INDEFINITE

        private const val minTextSize = 12
        private const val maxTextSize = 18
        private const val actionButtonIncreaseDps = 16
        private const val stepGranularity = 1

        /**
         * Display a snackbar in the given view with duration and proper normal/error styling.
         * Note: Duration is overriden for users with accessibility settings enabled
         * displayedOnFragmentWithToolbar should be true for all snackbars that will end up
         * being displayed on a BrowserFragment and must be true in cases where the fragment is
         * going to pop TO BrowserFragment (e.g. EditBookmarkFragment, ShareFragment)
         *
         * Suppressing ComplexCondition. Yes it's unfortunately complex but that's the nature
         * of the snackbar handling by Android. It will be simpler once dynamic toolbar is always on.
         */
        @Suppress("ComplexCondition")
        fun make(
            view: View,
            duration: Int = LENGTH_LONG,
            isError: Boolean = false,
            isDisplayedWithBrowserToolbar: Boolean
        ): FenixSnackbar {
            val parent = findSuitableParent(view) ?: run {
                throw IllegalArgumentException(
                    "No suitable parent found from the given view. Please provide a valid view."
                )
            }

            val inflater = LayoutInflater.from(parent.context)
            val content = inflater.inflate(R.layout.fenix_snackbar, parent, false)

            val durationOrAccessibleDuration = if (parent.context.settings().accessibilityServicesEnabled) {
                LENGTH_ACCESSIBLE
            } else {
                duration
            }

            val callback = FenixSnackbarCallback(content)
            val shouldUseBottomToolbar = view.context.settings().shouldUseBottomToolbar
            val toolbarHeight = view.context.resources
                .getDimensionPixelSize(R.dimen.browser_toolbar_height)

            return FenixSnackbar(parent, content, callback, isError).also {
                it.duration = durationOrAccessibleDuration

                it.view.setPadding(
                    0,
                    0,
                    0,
                    if (
                        isDisplayedWithBrowserToolbar &&
                        shouldUseBottomToolbar &&
                        // If the view passed in is a ContentFrameLayout, it does not matter
                        // if the user has a dynamicBottomToolbar or not, as the Android system
                        // can't intelligently position the snackbar on the upper most view.
                        // Ideally we should not pass ContentFrameLayout in, but it's the only
                        // way to display snackbars through a fragment transition.
                        view is ContentFrameLayout
                    ) {
                        toolbarHeight
                    } else {
                        0
                    }
                )
            }
        }

        // Use the same implementation of `Snackbar`
        private fun findSuitableParent(_view: View?): ViewGroup? {
            var view = _view
            var fallback: ViewGroup? = null

            do {
                if (view is CoordinatorLayout) {
                    return view
                }

                if (view is FrameLayout) {
                    if (view.id == android.R.id.content) {
                        return view
                    }

                    fallback = view
                }

                if (view != null) {
                    val parent = view.parent
                    view = if (parent is View) parent else null
                }
            } while (view != null)

            return fallback
        }
    }
}

private class FenixSnackbarCallback(
    private val content: View
) : ContentViewCallback {

    override fun animateContentIn(delay: Int, duration: Int) {
        content.translationY = (content.height).toFloat()
        content.animate().apply {
            translationY(defaultYTranslation)
            setDuration(animateInDuration)
            startDelay = delay.toLong()
        }
    }

    override fun animateContentOut(delay: Int, duration: Int) {
        content.translationY = defaultYTranslation
        content.animate().apply {
            translationY((content.height).toFloat())
            setDuration(animateOutDuration)
            startDelay = delay.toLong()
        }
    }

    companion object {
        private const val defaultYTranslation = 0f
        private const val animateInDuration = 200L
        private const val animateOutDuration = 150L
    }
}
