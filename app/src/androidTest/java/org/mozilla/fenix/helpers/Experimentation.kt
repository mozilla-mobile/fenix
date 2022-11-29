/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.helpers

import org.mozilla.experiments.nimbus.GleanPlumbMessageHelper
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.helpers.TestHelper.appContext

object Experimentation {
    val experiments =
        appContext.components.analytics.experiments

    fun withHelper(block: GleanPlumbMessageHelper.() -> Unit) {
        val helper = experiments.createMessageHelper()
        block(helper)
    }
}
