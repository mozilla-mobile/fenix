package org.mozilla.fenix.tabtray

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
