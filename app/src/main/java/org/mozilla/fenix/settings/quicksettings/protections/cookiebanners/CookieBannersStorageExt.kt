/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.quicksettings.protections.cookiebanners

import mozilla.components.browser.state.state.SessionState
import mozilla.components.concept.engine.cookiehandling.CookieBannersStorage
import org.mozilla.fenix.utils.Settings

suspend fun CookieBannersStorage.queryCookieBannerState(
    tab: SessionState,
    settings: Settings,
): CookieBannerState {
    return if (settings.shouldUseCookieBanner) {
        if (canHandleSite(tab.content.url, tab.content.private)) {
            if (!hasException(tab.content.url, tab.content.private)) {
                CookieBannerState.ON
            } else {
                CookieBannerState.OFF
            }
        } else {
            CookieBannerState.NO_SUPPORTED
        }
    } else {
        CookieBannerState.OFF
    }
}

suspend fun CookieBannersStorage.canHandleSite(uri: String, privateBrowsing: Boolean): Boolean {
    println(privateBrowsing)
    println(uri)
    return true
}
