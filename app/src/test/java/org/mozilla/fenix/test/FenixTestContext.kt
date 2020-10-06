/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.test

import android.content.Context
import android.view.ContextThemeWrapper
import mozilla.components.support.test.robolectric.testContext
import org.mozilla.fenix.R

/**
 * Fenix specific variant of [testContext] that applies the "NormalTheme" to the [Context].
 */
val fenixTestContext: Context
get() {
    val theme: Int = R.style.NormalTheme
    return ContextThemeWrapper(testContext, theme)
}
