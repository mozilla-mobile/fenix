/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Context
import androidx.navigation.NavController
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.feature.accounts.FirefoxAccountsAuthFeature
import mozilla.components.feature.app.links.AppLinksInterceptor
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.support.ktx.android.content.hasCamera
import org.mozilla.fenix.FeatureFlags
import org.mozilla.fenix.NavGraphDirections
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.settings.SupportUtils
import org.mozilla.fenix.test.Mockable

/**
 * Component group which encapsulates foreground-friendly services.
 */
@Mockable
class Services(
    private val context: Context,
    private val accountManager: FxaAccountManager
) {

    val fxaRedirectUrl = FxaServer.redirectUrl(context)

    val accountsAuthFeature by lazy {
        FirefoxAccountsAuthFeature(
            accountManager,
            redirectUrl = fxaRedirectUrl
        ) { context, authUrl ->
            CoroutineScope(Dispatchers.Main).launch {
                val intent = SupportUtils.createAuthCustomTabIntent(context, authUrl)
                context.startActivity(intent)
            }
        }
    }

    val appLinksInterceptor by lazy {
        AppLinksInterceptor(
            context,
            interceptLinkClicks = true,
            launchInApp = {
                PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                    context.getPreferenceKey(R.string.pref_key_open_links_in_external_app), false)
            }
        )
    }

    /**
     * Launches the sign in and pairing custom tab from any screen in the app.
     * @param context the current Context
     * @param navController the navController to use for navigation
     */
    fun launchPairingSignIn(context: Context, navController: NavController) {
        // Do not navigate to pairing UI if camera not available or pairing is disabled
        if (context.hasCamera() && !FeatureFlags.asFeatureFxAPairingDisabled) {
            val directions = NavGraphDirections.actionGlobalTurnOnSync()
            navController.navigate(directions)
        } else {
            context.components.services.accountsAuthFeature.beginAuthentication(context)
            // TODO The sign-in web content populates session history,
            // so pressing "back" after signing in won't take us back into the settings screen, but rather up the
            // session history stack.
            // We could auto-close this tab once we get to the end of the authentication process?
            // Via an interceptor, perhaps.
            context.components.analytics.metrics.track(Event.SyncAuthSignIn)
        }
    }
}
