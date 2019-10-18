/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.fenix.components

import android.content.Context
import mozilla.components.service.fxa.ServerConfig
import org.mozilla.fenix.Experiments
import org.mozilla.fenix.isInExperiment

/**
 * Utility to configure Firefox Account servers.
 */

object FxaServer {
    const val CLIENT_ID = "a2270f727f45f648"
    //const val REDIRECT_URL = "http://127.0.0.1:3030/oauth/success/$CLIENT_ID"
    const val REDIRECT_URL = "https://fpnfenix.dev.lcip.org/oauth/success/$CLIENT_ID"

    fun redirectUrl(context: Context) = if (context.isInExperiment(Experiments.asFeatureWebChannelsDisabled)) {
        REDIRECT_URL
    } else {
        REDIRECT_URL
    }

    fun config(context: Context): ServerConfig {
        return ServerConfig("https://fpnfenix.dev.lcip.org", CLIENT_ID, redirectUrl(context))
    }
}
