/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Context
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.fetch.Client
import mozilla.components.feature.customtabs.CustomTabIntentProcessor
import mozilla.components.feature.customtabs.store.CustomTabsServiceStore
import mozilla.components.feature.intent.processing.TabIntentProcessor
import mozilla.components.feature.pwa.ManifestStorage
import mozilla.components.feature.pwa.intent.WebAppIntentProcessor
import mozilla.components.feature.pwa.intent.TrustedWebActivityIntentProcessor
import mozilla.components.feature.search.SearchUseCases
import mozilla.components.feature.session.SessionUseCases
import org.mozilla.fenix.BuildConfig
import org.mozilla.fenix.test.Mockable

/**
 * Component group for miscellaneous components.
 */
@Mockable
class IntentProcessors(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val sessionUseCases: SessionUseCases,
    private val searchUseCases: SearchUseCases,
    private val httpClient: Client,
    private val customTabsStore: CustomTabsServiceStore
) {
    /**
     * Provides intent processing functionality for ACTION_VIEW and ACTION_SEND intents.
     */
    val intentProcessor by lazy {
        TabIntentProcessor(sessionManager, sessionUseCases.loadUrl, searchUseCases.newTabSearch, isPrivate = false)
    }

    /**
     * Provides intent processing functionality for ACTION_VIEW and ACTION_SEND intents in private tabs.
     */
    val privateIntentProcessor by lazy {
        TabIntentProcessor(sessionManager, sessionUseCases.loadUrl, searchUseCases.newTabSearch, isPrivate = true)
    }

    val customTabIntentProcessor by lazy {
        CustomTabIntentProcessor(sessionManager, sessionUseCases.loadUrl, context.resources, isPrivate = false)
    }

    val privateCustomTabIntentProcessor by lazy {
        CustomTabIntentProcessor(sessionManager, sessionUseCases.loadUrl, context.resources, isPrivate = true)
    }

    val externalAppIntentProcessors by lazy {
        listOf(
            TrustedWebActivityIntentProcessor(
                sessionManager = sessionManager,
                loadUrlUseCase = sessionUseCases.loadUrl,
                httpClient = httpClient,
                packageManager = context.packageManager,
                apiKey = BuildConfig.DIGITAL_ASSET_LINKS_TOKEN,
                store = customTabsStore
            ),
            WebAppIntentProcessor(sessionManager, sessionUseCases.loadUrl, ManifestStorage(context))
        )
    }
}
