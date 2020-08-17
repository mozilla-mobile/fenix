/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import android.widget.EditText

/**
 * Places cursor at the end of an EditText.
 */
fun EditText.placeCursorAtEnd() {
    this.text?.length?.let { setSelection(it, it) }
}
