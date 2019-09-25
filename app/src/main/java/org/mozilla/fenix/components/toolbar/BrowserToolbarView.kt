/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import kotlinx.android.synthetic.main.browser_toolbar_popup_window.view.*
import mozilla.components.browser.domains.autocomplete.ShippedDomainsProvider
import mozilla.components.browser.toolbar.BrowserToolbar
import org.mozilla.fenix.R
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.utils.ClipboardHandler

class BrowserToolbarView(
    container: ViewGroup,
    interactor: BrowserToolbarViewInteractor
) : BaseBrowserToolbarView(container, interactor, sessionId = null) {

    private val urlBackground = LayoutInflater.from(container.context)
        .inflate(R.layout.layout_url_background, container, false)

    override val toolbarIntegration: ToolbarIntegration

    init {
        val components = container.context.components
        toolbarIntegration = ToolbarIntegration(
            container.context,
            view,
            container,
            DefaultToolbarMenu(
                container.context,
                hasAccountProblem = components.backgroundServices.accountManager.accountNeedsReauth(),
                requestDesktopStateProvider = {
                    getSessionById()?.desktopMode ?: false
                },
                onItemTapped = { interactor.onBrowserToolbarMenuItemTapped(it) }
            ),
            ShippedDomainsProvider().also { it.initialize(container.context) },
            components.core.historyStorage,
            components.core.sessionManager,
            sessionId = null,
            isPrivate = getSessionById()?.private ?: false
        )

        styleBrowserToolbar(view)
    }

    override fun onUrlLongClick(customView: View, clipboard: ClipboardHandler) {
        super.onUrlLongClick(customView, clipboard)

        customView.paste.isGone = clipboard.text.isNullOrEmpty()
        customView.paste_and_go.isGone = clipboard.text.isNullOrEmpty()
    }

    override fun styleBrowserToolbar(toolbar: BrowserToolbar) {
        super.styleBrowserToolbar(toolbar)

        toolbar.urlBoxView = urlBackground
        toolbar.progressBarGravity = PROGRESS_TOP
    }
}
