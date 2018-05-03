/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.fenix.components

import android.content.Context
import mozilla.components.browser.engine.gecko.GeckoEngine
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.session.SessionProvider
import mozilla.components.concept.engine.Engine
import mozilla.components.feature.session.SessionMapping
import mozilla.components.feature.session.SessionUseCases
import mozilla.fenix.components.session.DefaultSessionProvider
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

/**
 * Helper class for lazily instantiating components needed by the application.
 */
class Components(private val applicationContext: Context) {
    private val geckoRuntime by lazy {
        val settings = GeckoRuntimeSettings()
        GeckoRuntime.create(applicationContext, settings)
    }

    val engine : Engine by lazy { GeckoEngine(geckoRuntime) }

    private val sessionProvider: SessionProvider by lazy { DefaultSessionProvider() }
    val sessionManager by lazy { SessionManager(sessionProvider) }
    val sessionMapping by lazy { SessionMapping() }
    val sessionUseCases by lazy { SessionUseCases(sessionManager, engine, sessionMapping) }
}
