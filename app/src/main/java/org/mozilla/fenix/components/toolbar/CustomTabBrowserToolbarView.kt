/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import kotlinx.android.synthetic.main.browser_toolbar_popup_window.view.*
import mozilla.components.browser.toolbar.BrowserToolbar
import org.mozilla.fenix.customtabs.CustomTabToolbarMenu
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.utils.ClipboardHandler

class CustomTabBrowserToolbarView(
    container: ViewGroup,
    interactor: BrowserToolbarViewInteractor,
    sessionId: String?
) : BaseBrowserToolbarView(container, interactor, sessionId) {

    override val toolbarIntegration = CustomTabToolbarIntegration(
        view.context,
        view,
        CustomTabToolbarMenu(
            view.context,
            view.context.components.core.sessionManager,
            sessionId,
            onItemTapped = {
                interactor.onBrowserToolbarMenuItemTapped(it)
            }
        ),
        sessionId,
        isPrivate = getSessionById()?.private ?: false
    )

    init {
        styleBrowserToolbar(view)
    }

    override fun onUrlLongClick(customView: View, clipboard: ClipboardHandler) {
        super.onUrlLongClick(customView, clipboard)

        customView.paste.isGone = true
        customView.paste_and_go.isGone = true
    }

    override fun styleBrowserToolbar(toolbar: BrowserToolbar) {
        super.styleBrowserToolbar(toolbar)

        toolbar.urlBoxView = null
        toolbar.progressBarGravity = PROGRESS_BOTTOM
    }
}
