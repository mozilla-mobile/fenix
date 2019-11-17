/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol

/**
 * Interface for tab related actions in the TabSessionInteractor.
 */
interface TabSessionInteractor {
    /**
     * Shows the Private Browsing Learn More page in a new tab. Called when a user clicks on the
     * "Common myths about private browsing" link in private mode.
     */
    fun onPrivateBrowsingLearnMoreClicked()

    /**
     * Saves the given tab to collection. Called when a user clicks on the "Save to collection"
     * button or tab header menu item, and on long click of an open tab.
     *
     * @param sessionId The selected tab session id to save.
     */
    fun onSaveToCollection(sessionId: String?)

    /**
     * Shares the current opened tabs. Called when a user clicks on the Share Tabs button in private
     * mode or tab header menu item.
     */
    fun onShareTabs()
}

/**
 * Interactor for the Home screen.
 * Provides implementations for the TabSessionInteractor.
 */
class SessionControlInteractor(
    private val controller: SessionControlController
) : TabSessionInteractor {
    override fun onPrivateBrowsingLearnMoreClicked() {
        controller.handlePrivateBrowsingLearnMoreClicked()
    }

    override fun onSaveToCollection(sessionId: String?) {
        controller.handleSaveTabToCollection(sessionId)
    }

    override fun onShareTabs() {
        controller.handleShareTabs()
    }
}
