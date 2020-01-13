/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.fenix.components

import android.content.Context
import mozilla.components.service.fxa.ServerConfig
import org.mozilla.fenix.FeatureFlags

/**
 * Utility to configure Firefox Account servers.
 */

object FxaServer {
    const val CLIENT_ID = "a2270f727f45f648"
    const val REDIRECT_URL = "https://accounts.firefox.com/oauth/success/$CLIENT_ID"

    @Suppress("ConstantConditionIf", "UNUSED_PARAMETER")
    fun redirectUrl(context: Context) = if (FeatureFlags.asFeatureWebChannelsDisabled) {
        REDIRECT_URL
    } else {
        "urn:ietf:wg:oauth:2.0:oob:oauth-redirect-webchannel"
    }

    fun config(context: Context): ServerConfig {
        return ServerConfig.release(CLIENT_ID, redirectUrl(context))
    }
}
