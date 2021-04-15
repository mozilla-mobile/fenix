/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.accounts

import android.content.Context
import org.mozilla.fenix.ext.components

open class FenixAccountManager(context: Context) {

    val accountManager = context.components.backgroundServices.accountManager
    val authenticatedAccount = accountManager.authenticatedAccount() != null
    val accountProfile = accountManager.accountProfile()

    fun getAuthAccountEmail(): String? {
        val email = accountProfile?.email
        return if (authenticatedAccount && email != null) email else null
    }
}