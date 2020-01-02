/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser

import android.view.View
import androidx.annotation.StringRes
import mozilla.components.feature.contextmenu.ContextMenuCandidate
import org.mozilla.fenix.components.FenixSnackbar

class FenixSnackbarDelegate(val view: View) :
    ContextMenuCandidate.SnackbarDelegate {
    override fun show(
        snackBarParentView: View,
        @StringRes text: Int,
        duration: Int,
        @StringRes action: Int,
        listener: ((v: View) -> Unit)?
    ) {
        if (listener != null && action != 0) {
            FenixSnackbar.makeWithToolbarPadding(view)
                .setText(view.context.getString(text))
                .setAction(view.context.getString(action)) { listener.invoke(view) }
                .show()
        } else {
            FenixSnackbar.makeWithToolbarPadding(view)
                .setText(view.context.getString(text))
                .show()
        }
    }
}
