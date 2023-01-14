/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.quicksettings.protections.cookiebanners

import mozilla.components.browser.state.state.SessionState
import mozilla.components.concept.engine.cookiehandling.CookieBannersStorage
import mozilla.components.support.base.log.logger.Logger

/**
 * Queries [CookieBannersStorage.hasException] and handles unexpected exceptions.
 */
@Suppress("TooGenericExceptionCaught")
suspend inline fun CookieBannersStorage.safeHasException(tab: SessionState, logger: Logger): Boolean {
    return try {
        hasException(
            tab.content.url,
            tab.content.private,
        )
    } catch (e: Exception) {
        // This normally happen on internal sites like about:config
        logger.error("Unable to query cookie banners exception")
        false
    }
}
