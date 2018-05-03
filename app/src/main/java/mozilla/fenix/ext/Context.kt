/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.fenix.ext

import android.content.Context
import mozilla.fenix.components.Components
import mozilla.fenix.FenixApplication

/**
 * Get the FenixApplication object from a context.
 */
val Context.application: FenixApplication
    get() = applicationContext as FenixApplication

/**
 * Get the components of this application.
 */
val Context.components: Components
    get() = application.components
