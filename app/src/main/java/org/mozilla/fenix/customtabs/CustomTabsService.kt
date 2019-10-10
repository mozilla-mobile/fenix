/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.customtabs

import mozilla.components.concept.engine.Engine
import mozilla.components.feature.customtabs.AbstractCustomTabsService
import org.mozilla.fenix.BuildConfig.DIGITAL_ASSET_LINKS_TOKEN
import org.mozilla.fenix.ext.components

class CustomTabsService : AbstractCustomTabsService() {
    override val engine: Engine by lazy { applicationContext.components.core.engine }
    override val customTabsServiceStore by lazy { applicationContext.components.core.customTabsStore }
    override val httpClient by lazy { applicationContext.components.core.client }
    override val apiKey: String? = DIGITAL_ASSET_LINKS_TOKEN
}
