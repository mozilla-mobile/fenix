/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.intent

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS
import androidx.navigation.NavController
import mozilla.components.concept.engine.EngineSession
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.GlobalDirections
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.ext.alreadyOnDestination

/**
 * Deep links in the form of `fenix://host` open different parts of the app.
 *
 * @param verifier [DeepLinkVerifier] that will be used to verify deep links before handling them.
 */
class DeepLinkIntentProcessor(
    private val activity: HomeActivity,
    private val verifier: DeepLinkVerifier
) : HomeIntentProcessor {
    private val logger = Logger("DeepLinkIntentProcessor")

    override fun process(intent: Intent, navController: NavController, out: Intent): Boolean {
        val scheme = intent.scheme?.contains("fenix") ?: return false
        return if (scheme) {
            intent.data?.let { handleDeepLink(it, navController) }
            true
        } else {
            false
        }
    }

    @Suppress("ComplexMethod")
    private fun handleDeepLink(deepLink: Uri, navController: NavController) {
        if (!verifier.verifyDeepLink(deepLink)) {
            logger.warn("Invalid deep link: $deepLink")
            return
        }

        handleDeepLinkSideEffects(deepLink)

        val globalDirections = when (deepLink.host) {
            "home", "enable_private_browsing" -> GlobalDirections.Home
            "settings" -> GlobalDirections.Settings
            "turn_on_sync" -> GlobalDirections.Sync
            "settings_search_engine" -> GlobalDirections.SearchEngine
            "settings_accessibility" -> GlobalDirections.Accessibility
            "settings_delete_browsing_data" -> GlobalDirections.DeleteData
            "settings_addon_manager" -> GlobalDirections.SettingsAddonManager
            else -> return
        }

        if (!navController.alreadyOnDestination(globalDirections.destinationId)) {
            navController.navigate(globalDirections.navDirections)
        }
    }

    /**
     * Handle links that require more than just simple navigation.
     */
    private fun handleDeepLinkSideEffects(deepLink: Uri) {
        when (deepLink.host) {
            "enable_private_browsing" -> {
                activity.browsingModeManager.mode = BrowsingMode.Private
            }
            "make_default_browser" -> {
                if (SDK_INT >= Build.VERSION_CODES.N) {
                    val settingsIntent = Intent(ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                    activity.startActivity(settingsIntent)
                }
            }
            "open" -> {
                val url = deepLink.getQueryParameter("url")
                if (url == null || !url.startsWith("https://")) {
                    logger.info("Not opening deep link: $url")
                    return
                }

                activity.openToBrowserAndLoad(
                    url,
                    newTab = true,
                    from = BrowserDirection.FromGlobal,
                    flags = EngineSession.LoadUrlFlags.external()
                )
            }
        }
    }

    /**
     * Interface for a class that verifies deep links before they get handled.
     */
    interface DeepLinkVerifier {
        /**
         * Verifies the given deep link and returns `true` for verified deep links or `false` for
         * rejected deep links.
         */
        fun verifyDeepLink(deepLink: Uri): Boolean
    }
}
