/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.accounts

import android.content.Context
import mozilla.components.service.fxa.manager.FxaAccountManager
import org.mozilla.fenix.ext.components

/**
 * Component which holds a reference to [FxaAccountManager]. Manages account authentication,
 * profiles, and profile state observers.
 */
open class FenixAccountManager(context: Context) {
    val accountManager = context.components.backgroundServices.accountManager
    val authenticatedAccount = accountManager.authenticatedAccount() != null

    /**
     * Get the email address associated with the authenticated account profile. Returns null if
     * the account is not authenticated or the email address is null.
     */
    fun getAuthAccountEmail(): String? {
        val email = accountManager.accountProfile()?.email
        return if (authenticatedAccount && !email.isNullOrEmpty()) email else null
    }

    /**
     * Check if the current account is signed in and authenticated.
     */
    fun signedInToFxa(): Boolean {
        val account = accountManager.authenticatedAccount()
        val needsReauth = accountManager.accountNeedsReauth()

        return account != null && !needsReauth
    }
}
