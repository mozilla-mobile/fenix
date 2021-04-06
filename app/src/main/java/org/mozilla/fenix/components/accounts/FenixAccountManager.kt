/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.accounts

import android.content.Context
import mozilla.components.service.fxa.manager.FxaAccountManager
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
 * Contains helper methods for querying Firefox Account state and its properties.
 */
class FenixAccountManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private val accountManager = context.components.backgroundServices.accountManager

    /**
     * Returns the Firefox Account email if authenticated in the app, `null` otherwise.
     */
    val accountProfileEmail: String?
        get() = if (accountState == AccountState.AUTHENTICATED) {
            accountManager.accountProfile()?.email
        } else {
            null
        }

    /**
     * The current state of the Firefox Account. See [AccountState].
     */
    val accountState: AccountState
        get() = if (accountManager.authenticatedAccount() == null) {
            AccountState.NO_ACCOUNT
        } else {
            if (accountManager.accountNeedsReauth()) {
                AccountState.NEEDS_REAUTHENTICATION
            } else {
                AccountState.AUTHENTICATED
            }
        }


    /**
     * Check if the current account is signed in and authenticated.
     */
    fun signedInToFxa(): Boolean {
        val account = accountManager.authenticatedAccount()
        val needsReauth = accountManager.accountNeedsReauth()

        return account != null && !needsReauth
    }

    /**
     * Observe account state and updates menus.
     */
    fun observeAccountState(
        menuItems: List<BrowserMenuItem>,
        onMenuBuilderChanged: (BrowserMenuBuilder) -> Unit = {}
    ) {
        context.components.backgroundServices.accountManagerAvailableQueue.runIfReadyOrQueue {
            if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                return@runIfReadyOrQueue
            }
            context.components.backgroundServices.accountManager.register(object : AccountObserver {
                override fun onAuthenticationProblems() {
                    lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                        onMenuBuilderChanged(BrowserMenuBuilder(menuItems))
                    }
                }

                override fun onAuthenticated(account: OAuthAccount, authType: AuthType) {
                    lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                        onMenuBuilderChanged(BrowserMenuBuilder(menuItems))
                    }
                }

                override fun onLoggedOut() {
                    lifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                        onMenuBuilderChanged(BrowserMenuBuilder(menuItems))
                    }
                }
            }, lifecycleOwner)
        }
    }
}

/**
 * General states as an overview of the current Firefox Account.
 */
enum class AccountState {
    /**
     * There is no known Firefox Account.
     */
    NO_ACCOUNT,

    /**
     * A Firefox Account exists but needs to be re-authenticated.
     */
    NEEDS_REAUTHENTICATION,

    /**
     * A Firefox Account exists and the user is currently signed into it.
     */
    AUTHENTICATED,
}
