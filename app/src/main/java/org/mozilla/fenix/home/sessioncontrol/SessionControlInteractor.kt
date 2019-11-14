/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol

/**
 * Interface for tab related actions in the TabSessionInteractor.
 */
interface TabSessionInteractor {
    /**
     * Saves the given tab to collection. Called when a user clicks on the "Save to collection"
     * button or tab header menu item, and on long click of an open tab.
     *
     * @param sessionId The selected tab session id to save.
     */
    fun onSaveToCollection(sessionId: String?)
}

/**
 * Interactor for the Home screen.
 * Provides implementations for the TabSessionInteractor.
 */
class SessionControlInteractor(
    private val controller: SessionControlController
) : TabSessionInteractor {
    override fun onSaveToCollection(sessionId: String?) {
        controller.handleSaveTabToCollection(sessionId)
    }
}
