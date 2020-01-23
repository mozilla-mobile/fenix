/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.addons

import android.view.View
import org.mozilla.fenix.components.FenixSnackbar
import java.text.NumberFormat
import java.util.Locale

internal fun getFormattedAmount(amount: Int): String {
    return NumberFormat.getNumberInstance(Locale.getDefault()).format(amount)
}

internal fun showSnackBar(view: View, text: String) {
    FenixSnackbar.make(view, FenixSnackbar.LENGTH_SHORT)
        .setText(text)
        .show()
}
