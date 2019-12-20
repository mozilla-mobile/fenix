/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabtray

/**
 * Interactor for the tab tray screen
 * Provides implementations for the TabTrayInteractor
 */
@SuppressWarnings("TooManyFunctions")
class TabTrayInteractor(
    private val controller: TabTrayController
) : TabTrayViewInteractor {
    override fun normalModeButtonTapped() { controller.exitPrivateBrowsingMode() }
    override fun privateModeButtonTapped() { controller.enterPrivateBrowsingMode() }
    override fun closeAllTabsTapped() { controller.closeAllTabs() }
    override fun newTabTapped() { controller.newTab() }
    override fun closeButtonTapped(tab: Tab) { controller.closeTab(tab) }
    override fun onPauseMediaClicked() { controller.pauseMedia() }
    override fun onPlayMediaClicked() { controller.playMedia() }
    override fun open(item: Tab) { controller.openTab(item) }
    override fun select(item: Tab) { controller.selectTab(item) }
    override fun shouldAllowSelect(): Boolean = controller.shouldAllowSelect()
    override fun deselect(item: Tab) { controller.deselectTab(item) }
}
