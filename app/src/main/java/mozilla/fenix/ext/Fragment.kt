/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.fenix.ext

import android.support.v4.app.Fragment
import mozilla.fenix.components.Components

/**
 * Get the components of this application.
 */
val Fragment.components: Components
    get() = activity!!.components
