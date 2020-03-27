/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.media

import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.media.service.AbstractMediaService
import org.mozilla.fenix.ext.components

/**
 * [AbstractMediaService] implementation for injecting [BrowserStore] singleton.
 */
class MediaService : AbstractMediaService() {
    override val store: BrowserStore by lazy { components.core.store }
}
