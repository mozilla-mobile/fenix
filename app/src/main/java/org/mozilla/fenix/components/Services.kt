/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import mozilla.components.feature.accounts.FirefoxAccountsAuthFeature
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.service.fxa.FxaAccountManager

/**
 * Component group which encapsulates foreground-friendly services.
 */
class Services(
    private val accountManager: FxaAccountManager,
    private val tabsUseCases: TabsUseCases
) {
    val accountsAuthFeature by lazy {
        FirefoxAccountsAuthFeature(
            accountManager,
            tabsUseCases,
            redirectUrl = BackgroundServices.REDIRECT_URL
        )
    }
}
