/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding.interactor

interface OnboardingInteractor {
    /**
     * Hides the onboarding and navigates to Search. Called when a user clicks on the "Start Browsing" button.
     */
    fun onStartBrowsingClicked()

    /**
     * Opens a custom tab to privacy notice url. Called when a user clicks on the "read our privacy notice" button.
     */
    fun onReadPrivacyNoticeClicked()

    /**
     * Show the onboarding dialog to onboard users about recentTabs,recentBookmarks,
     * historyMetadata and pocketArticles sections.
     */
    fun showOnboardingDialog()
}

class DefaultOnboardingInteractor(

) : OnboardingInteractor {

    override fun onStartBrowsingClicked() {
        TODO("Not yet implemented")
    }

    override fun onReadPrivacyNoticeClicked() {
        TODO("Not yet implemented")
    }

    override fun showOnboardingDialog() {
        TODO("Not yet implemented")
    }
}
