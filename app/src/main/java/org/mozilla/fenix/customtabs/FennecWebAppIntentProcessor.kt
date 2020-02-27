/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.customtabs

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_DOCUMENT
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.Session.Source
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.state.state.CustomTabConfig
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.manifest.WebAppManifest
import mozilla.components.concept.engine.manifest.WebAppManifestParser
import mozilla.components.concept.engine.manifest.getOrNull
import mozilla.components.feature.intent.ext.putSessionId
import mozilla.components.feature.intent.processing.IntentProcessor
import mozilla.components.feature.pwa.ManifestStorage
import mozilla.components.feature.pwa.ext.putWebAppManifest
import mozilla.components.feature.pwa.ext.toCustomTabConfig
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.support.utils.SafeIntent
import mozilla.components.support.utils.toSafeIntent
import org.json.JSONObject
import org.mozilla.fenix.R
import java.io.File
import java.io.IOException

/**
 * Legacy processor for Progressive Web App shortcut intents created by Fennec.
 */
class FennecWebAppIntentProcessor(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val loadUrlUseCase: SessionUseCases.DefaultLoadUrlUseCase,
    private val storage: ManifestStorage
) : IntentProcessor {

    /**
     * Returns true if this intent should launch a progressive web app created in Fennec.
     */
    override fun matches(intent: Intent) =
        intent.toSafeIntent().action == ACTION_FENNEC_WEBAPP

    /**
     * Processes the given [Intent] by creating a [Session] with a corresponding web app manifest.
     *
     * A custom tab config is also set so a custom tab toolbar can be shown when the user leaves
     * the scope defined in the manifest.
     */
    override suspend fun process(intent: Intent): Boolean {
        val safeIntent = intent.toSafeIntent()
        val url = safeIntent.dataString

        return if (!url.isNullOrEmpty() && matches(intent)) {
            val webAppManifest = loadManifest(safeIntent, url)

            val session = Session(url, private = false, source = Source.HOME_SCREEN)
            session.webAppManifest = webAppManifest
            session.customTabConfig = webAppManifest?.toCustomTabConfig() ?: createFallbackCustomTabConfig()

            sessionManager.add(session)
            loadUrlUseCase(url, session, EngineSession.LoadUrlFlags.external())

            intent.putSessionId(session.id)

            if (webAppManifest != null) {
                intent.flags = FLAG_ACTIVITY_NEW_DOCUMENT
                intent.putWebAppManifest(webAppManifest)
            }

            true
        } else {
            false
        }
    }

    private suspend fun loadManifest(intent: SafeIntent, url: String): WebAppManifest? {
        // Load from our manifest storage: If we already loaded this manifest from the original
        // Fennec file then it is in our storage now and that should have precedence.
        storage.loadManifest(url)?.let {
            return it
        }

        // Let's try to read the file where Fennec used to store the manifest.
        fromFile(intent.getStringExtra(EXTRA_FENNEC_MANIFEST_PATH))?.also {
            storage.saveManifest(it)
            return it
        }

        // Both reads failed... let's continue without manifest.
        return null
    }

    @VisibleForTesting
    internal fun fromFile(path: String?): WebAppManifest? {
        if (path.isNullOrEmpty()) return null

        return try {
            // Gecko in Fennec added some add some additional data, such as cached_icon, in
            // the toplevel object. The actual web app manifest is in the "manifest" field.
            val manifest = JSONObject(File(path).readText())
            val manifestField = manifest.getJSONObject("manifest")

            WebAppManifestParser().parse(manifestField).getOrNull()
        } catch (e: IOException) {
            null
        }
    }

    private fun createFallbackCustomTabConfig(): CustomTabConfig {
        return CustomTabConfig(
            toolbarColor = ContextCompat.getColor(context, R.color.toolbar_center_gradient_normal_theme)
        )
    }

    companion object {
        const val ACTION_FENNEC_WEBAPP = "org.mozilla.gecko.WEBAPP"
        const val EXTRA_FENNEC_MANIFEST_PATH = "MANIFEST_PATH"
    }
}
