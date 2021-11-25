/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.onboarding.controller

interface OnboardingController {
    /**
     * @see [OnboardingInteractor.onStartBrowsingClicked]
     */
    fun handleStartBrowsingClicked()

    /**
     * @see [OnboardingInteractor.onReadPrivacyNoticeClicked]
     */
    fun handleReadPrivacyNoticeClicked()

    /**
     * @see [OnboardingInteractor.showOnboardingDialog]
     */
    fun handleShowOnboardingDialog()
}

class DefaultOnboardingController(

) : OnboardingController {

    override fun handleStartBrowsingClicked() {
        TODO("Not yet implemented")
    }

    override fun handleReadPrivacyNoticeClicked() {
        TODO("Not yet implemented")
    }

    override fun handleShowOnboardingDialog() {
        TODO("Not yet implemented")
    }
}
