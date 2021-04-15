/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.accounts

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.browser.menu.BrowserMenuBuilder
import mozilla.components.browser.menu.BrowserMenuItem
import mozilla.components.concept.sync.AccountObserver
import mozilla.components.concept.sync.AuthType
import mozilla.components.concept.sync.OAuthAccount
import org.mozilla.fenix.ext.components

/**
 * TODO
 *
 */
open class FenixAccountManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    val accountManager = context.components.backgroundServices.accountManager
    val authenticatedAccount = accountManager.authenticatedAccount() != null
    val accountProfile = accountManager.accountProfile()

    /**
     * Get the email address associated with the authenticated account profile. Returns null if
     * the account is not authenticated or the email address is null.
     */
    fun getAuthAccountEmail(): String? {
        val email = accountProfile?.email
        return if (authenticatedAccount && email != null) email else null
    }

    /**
     * TODO
     * Observes account state and updates menus
     */
    fun observeAccountState(
        menuItemsWithReconnectItem: List<BrowserMenuItem>,
        menuItems: List<BrowserMenuItem>,
        onMenuBuilderChanged: (BrowserMenuBuilder) -> Unit = {}
    ) {
        context.components.backgroundServices.accountManagerAvailableQueue.runIfReadyOrQueue {
            // This task isn't relevant if our parent fragment isn't around anymore.
            if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                return@runIfReadyOrQueue
            }
            context.components.backgroundServices.accountManager.register(object : AccountObserver {
                override fun onAuthenticationProblems() {
                    lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                        onMenuBuilderChanged(
                            BrowserMenuBuilder(
                                menuItemsWithReconnectItem
                            )
                        )
                    }
                }

                override fun onAuthenticated(account: OAuthAccount, authType: AuthType) {
                    lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                        onMenuBuilderChanged(
                            BrowserMenuBuilder(
                                menuItems
                            )
                        )
                    }
                }

                override fun onLoggedOut() {
                    lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                        onMenuBuilderChanged(
                            BrowserMenuBuilder(
                                menuItems
                            )
                        )
                    }
                }
            }, lifecycleOwner)
        }
    }
}