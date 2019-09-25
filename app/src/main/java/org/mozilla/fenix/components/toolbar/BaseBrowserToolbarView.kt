/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.browser_toolbar_popup_window.view.*
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.support.ktx.android.util.dpToFloat
import mozilla.components.support.ktx.android.util.dpToPx
import org.jetbrains.anko.dimen
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.theme.ThemeManager
import org.mozilla.fenix.utils.ClipboardHandler

interface BrowserToolbarViewInteractor {
    fun onBrowserToolbarPaste(text: String)
    fun onBrowserToolbarPasteAndGo(text: String)
    fun onBrowserToolbarClicked()
    fun onBrowserToolbarMenuItemTapped(item: ToolbarMenu.Item)
}

abstract class BaseBrowserToolbarView(
    final override val containerView: ViewGroup,
    val interactor: BrowserToolbarViewInteractor,
    private val sessionId: String?
) : LayoutContainer {

    val view: BrowserToolbar = LayoutInflater.from(containerView.context)
        .inflate(R.layout.component_search, containerView, true)
        .findViewById(R.id.toolbar)

    abstract val toolbarIntegration: BaseToolbarIntegration

    init {
        view.setOnUrlLongClickListener { view ->
            val clipboard = view.context.components.clipboardHandler
            val customView = LayoutInflater
                .from(view.context)
                .inflate(R.layout.browser_toolbar_popup_window, null)

            onUrlLongClick(customView, clipboard)
            true
        }
    }

    protected open fun onUrlLongClick(customView: View, clipboard: ClipboardHandler) {
        val popupWindow = PopupWindow(
            customView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            view.context.dimen(R.dimen.context_menu_height),
            true
        )

        popupWindow.elevation = view.context.dimen(R.dimen.mozac_browser_menu_elevation).toFloat()

        customView.copy.setOnClickListener {
            popupWindow.dismiss()

            clipboard.text = getSessionById()?.url
        }

        customView.paste.setOnClickListener {
            popupWindow.dismiss()
            interactor.onBrowserToolbarPaste(clipboard.text!!)
        }

        customView.paste_and_go.setOnClickListener {
            popupWindow.dismiss()
            interactor.onBrowserToolbarPasteAndGo(clipboard.text!!)
        }

        popupWindow.showAsDropDown(view, view.context.dimen(R.dimen.context_menu_x_offset), 0, Gravity.START)
    }

    protected open fun styleBrowserToolbar(toolbar: BrowserToolbar) {
        toolbar.apply {
            elevation = TOOLBAR_ELEVATION.dpToFloat(resources.displayMetrics)

            onUrlClicked = {
                interactor.onBrowserToolbarClicked()
                false
            }

            browserActionMargin = browserActionMarginDp.dpToPx(resources.displayMetrics)

            textColor = ContextCompat.getColor(context, R.color.photonGrey30)

            hint = context.getString(R.string.search_hint)

            suggestionBackgroundColor = ContextCompat.getColor(
                containerView.context,
                R.color.suggestion_highlight_color
            )

            textColor = ContextCompat.getColor(
                containerView.context,
                ThemeManager.resolveAttribute(R.attr.primaryText, containerView.context)
            )

            hintColor = ContextCompat.getColor(
                containerView.context,
                ThemeManager.resolveAttribute(R.attr.secondaryText, containerView.context)
            )
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun update(state: BrowserFragmentState) {
        // Intentionally leaving this as a stub for now since we don't actually want to update currently
    }

    protected fun getSessionById() = view.context.components.core.sessionManager.run {
        sessionId?.let { findSessionById(it) } ?: selectedSession
    }

    companion object {
        internal const val TOOLBAR_ELEVATION = 16
        internal const val PROGRESS_BOTTOM = 0
        internal const val PROGRESS_TOP = 1
        const val browserActionMarginDp = 8
    }
}
