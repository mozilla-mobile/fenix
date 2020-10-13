/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.fenix.components

import android.content.Context
import android.util.Log
import mozilla.components.service.fxa.ServerConfig
import mozilla.components.service.fxa.ServerConfig.Server
import org.mozilla.fenix.ext.settings
import java.io.File

/**
 * Utility to configure Firefox Account servers.
 */

object FxaServer {
    private const val CLIENT_ID = "a2270f727f45f648"
    const val REDIRECT_URL = "urn:ietf:wg:oauth:2.0:oob:oauth-redirect-webchannel"

    fun config(context: Context): ServerConfig {
        val serverOverride = context.settings().overrideFxAServer
        val tokenServerOverride = context.settings().overrideSyncTokenServer.ifEmpty { null }

        // Try to read Fennec FxA state from fxa.aacount.json
        val haveReadFxAAccountJson = context.settings().haveReadFxAAccountJson
        if (!haveReadFxAAccountJson) {
            val fxaState = File("${context.filesDir}", "fxa.account.json")
            if (fxaState.exists()) {
                if (!fxaState.readText().contains("firefox.com.cn") && (context.settings().useLocalFxAServer)) {
                    context.settings().switchUseLocalFxAServer()
                }
            } else {
                Log.e("FxAaccount", "No fxa.account.json file!")
            }
            context.settings().switchHaveReadFxAAccountJson()
        }
        val useLocalFxAServer = context.settings().useLocalFxAServer

        if (serverOverride.isEmpty()) {
            // Figure out if we enable local server
            if (useLocalFxAServer) {
                return ServerConfig(Server.CHINA, CLIENT_ID, REDIRECT_URL, tokenServerOverride)
            }
            return ServerConfig(Server.RELEASE, CLIENT_ID, REDIRECT_URL, tokenServerOverride)
        }
        return ServerConfig(serverOverride, CLIENT_ID, REDIRECT_URL, tokenServerOverride)
    }
}
