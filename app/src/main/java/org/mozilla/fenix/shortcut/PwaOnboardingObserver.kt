/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.shortcut

import androidx.navigation.NavController
import mozilla.components.browser.session.Session
import mozilla.components.feature.pwa.WebAppUseCases
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.BrowserFragmentDirections
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.utils.Settings

/**
 * Displays the [PwaOnboardingDialogFragment] info dialog when a PWA is opened in the browser for the third time.
 */
class PwaOnboardingObserver(
    private val navController: NavController,
    private val settings: Settings,
    private val webAppUseCases: WebAppUseCases
) : Session.Observer {

    override fun onLoadingStateChanged(session: Session, loading: Boolean) {
        if (!loading && webAppUseCases.isInstallable() && !settings.userKnowsAboutPwas) {
            settings.incrementVisitedInstallableCount()
            if (settings.shouldShowPwaCfr) {
                val directions =
                    BrowserFragmentDirections.actionBrowserFragmentToPwaOnboardingDialogFragment()
                navController.nav(R.id.browserFragment, directions)
                settings.lastCfrShownTimeInMillis = System.currentTimeMillis()
                settings.userKnowsAboutPwas = true
            }
        }
    }
}
