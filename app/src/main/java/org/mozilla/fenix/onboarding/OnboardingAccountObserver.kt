/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding

import android.content.Context
import mozilla.components.concept.sync.AccountObserver
import mozilla.components.concept.sync.AuthType
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.concept.sync.Profile
import org.mozilla.fenix.ext.components

class OnboardingAccountObserver(
    private val context: Context,
    private val dispatchChanges: (state: OnboardingState) -> Unit,
) : AccountObserver {

    private val accountManager by lazy { context.components.backgroundServices.accountManager }

    override fun onAuthenticated(account: OAuthAccount, authType: AuthType) =
        dispatchChanges(getOnboardingState())

    override fun onAuthenticationProblems() = dispatchChanges(getOnboardingState())

    override fun onLoggedOut() = dispatchChanges(getOnboardingState())

    override fun onProfileUpdated(profile: Profile) = dispatchChanges(getOnboardingState())

    fun getOnboardingState(): OnboardingState {
        val account = accountManager.authenticatedAccount()

        return if (account != null) {
            OnboardingState.SignedIn
        } else {
            OnboardingState.SignedOutNoAutoSignIn
        }
    }
}
