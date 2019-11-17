/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol

import android.view.View

/**
 * Interface for tab related actions in the TabSessionInteractor.
 */
interface TabSessionInteractor {
    /**
     * Pauses all playing [Media]. Called when a user clicks on the Pause button in the tab view.
     */
    fun onPauseMediaClicked()

    /**
     * Resumes playing all paused [Media]. Called when a user clicks on the Play button in the tab
     * view.
     */
    fun onPlayMediaClicked()

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
     * Selects the given tab. Called when a user clicks on a tab.
     *
     * @param tabView [View] of the current Fragment to match with a View in the Fragment being
     *                navigated to.
     * @param sessionId The tab session id to select.
     */
    fun onSelectTab(tabView: View, sessionId: String)

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
    override fun onPauseMediaClicked() {
        controller.handlePauseMediaClicked()
    }

    override fun onPlayMediaClicked() {
        controller.handlePlayMediaClicked()
    }

    override fun onPrivateBrowsingLearnMoreClicked() {
        controller.handlePrivateBrowsingLearnMoreClicked()
    }

    override fun onSaveToCollection(sessionId: String?) {
        controller.handleSaveTabToCollection(sessionId)
    }

    override fun onSelectTab(tabView: View, sessionId: String) {
        controller.handleSelectTab(tabView, sessionId)
    }

    override fun onShareTabs() {
        controller.handleShareTabs()
    }
}
