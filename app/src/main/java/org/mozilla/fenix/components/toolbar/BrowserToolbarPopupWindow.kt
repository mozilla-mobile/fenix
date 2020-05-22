/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.PopupWindow
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.browser_toolbar_popup_window.*
import mozilla.components.browser.state.state.CustomTabSessionState
import mozilla.components.browser.state.state.SessionState
import org.mozilla.fenix.R
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.utils.ClipboardHandler

/**
 * Popup window for the browser toolbar that shows copy & paste actions.
 * Should be displayed when long pressing on the toolbar.
 */
class BrowserToolbarPopupWindow(
    context: Context,
    private val tab: SessionState?,
    private val clipboard: ClipboardHandler,
    private val interactor: BrowserToolbarViewInteractor
) : LayoutContainer {

    private val popupWindow: PopupWindow
    private var anchor: View? = null

    override val containerView: View = LayoutInflater.from(context)
        .inflate(R.layout.browser_toolbar_popup_window, null)

    init {
        popupWindow = PopupWindow(
            containerView,
            WRAP_CONTENT,
            context.resources.getDimensionPixelSize(R.dimen.context_menu_height),
            true
        ).apply {
            elevation = context.resources.getDimension(R.dimen.mozac_browser_menu_elevation)

            // This is a workaround for SDK<23 to allow popup dismissal on outside or back button press
            // See: https://github.com/mozilla-mobile/fenix/issues/10027
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        copy.setOnClickListener(::copy)
        paste.setOnClickListener {
            popupWindow.dismiss()
            interactor.onBrowserToolbarPaste(clipboard.text!!)
        }
        paste.setOnClickListener {
            popupWindow.dismiss()
            interactor.onBrowserToolbarPasteAndGo(clipboard.text!!)
        }
    }

    /**
     * Display the browser toolbar popup.
     */
    fun showAsDropDown(anchor: View) {
        val pasteVisibility =
            if (tab is CustomTabSessionState || clipboard.text.isNullOrEmpty()) GONE else VISIBLE
        paste.visibility = pasteVisibility
        paste_and_go.visibility = pasteVisibility

        this.anchor = anchor
        val xOffset = anchor.context.resources.getDimensionPixelSize(R.dimen.context_menu_x_offset)
        val yOffset = 0
        popupWindow.showAsDropDown(anchor, xOffset, yOffset, Gravity.START)
    }

    private fun copy(v: View) {
        popupWindow.dismiss()
        clipboard.text = tab?.content?.url

        FenixSnackbar.make(
            view = anchor!!,
            duration = Snackbar.LENGTH_SHORT,
            isDisplayedWithBrowserToolbar = true
        )
            .setText(v.context.getString(R.string.browser_toolbar_url_copied_to_clipboard_snackbar))
            .show()
    }
}
