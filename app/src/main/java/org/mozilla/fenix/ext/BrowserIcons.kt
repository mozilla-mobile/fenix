/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.icons.IconRequest

fun BrowserIcons.loadIntoView(
    view: ImageView,
    url: String = "",
    localImage: Drawable? = null,
    iconCover: ImageView? = null
): Job? {
    if (localImage != null) view.setImageDrawable(localImage)
    GlobalScope.launch {
        if (url.isNotBlank()) {
            loadIntoView(view = view, request = IconRequest(url))
        }
        delay(DELAY_UNTIL_REVEAL)
        withContext(Dispatchers.Main) {
            iconCover?.visibility = View.INVISIBLE
        }
    }
    return null
}

const val DELAY_UNTIL_REVEAL = 250L
