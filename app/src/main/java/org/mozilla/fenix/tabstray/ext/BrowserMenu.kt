/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.ext

import android.view.View
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import mozilla.components.browser.menu.BrowserMenu
import org.mozilla.fenix.R
import org.mozilla.fenix.theme.ThemeManager

/**
 * Invokes [BrowserMenu.show] and applies the default theme color background.
 */
fun BrowserMenu.showWithTheme(view: View) {
    show(view).also { popupMenu ->
        val color = ThemeManager.resolveAttribute(R.attr.layer2, view.context)
        (popupMenu.contentView as? CardView)?.setCardBackgroundColor(
            ContextCompat.getColor(view.context, color),
        )
    }
}
