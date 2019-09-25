/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import android.content.Context
import android.view.Gravity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.feature.toolbar.ToolbarFeature

class CustomTabToolbarIntegration(
    context: Context,
    toolbar: BrowserToolbar,
    toolbarMenu: ToolbarMenu,
    sessionId: String? = null,
    isPrivate: Boolean
) : BaseToolbarIntegration(
    context,
    toolbar,
    toolbarMenu,
    ToolbarFeature.RenderStyle.RegistrableDomain,
    sessionId,
    isPrivate,
    displaySeperator = false
) {

    init {
        toolbar.private = isPrivate

        // Remove toolbar shadow
        toolbar.elevation = 0f

        // Make the toolbar go to the top.
        (toolbar.layoutParams as CoordinatorLayout.LayoutParams).gravity = Gravity.TOP
    }
}
