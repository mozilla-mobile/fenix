/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import android.content.Intent
import androidx.annotation.VisibleForTesting
import androidx.navigation.NavController
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mozilla.appservices.places.BookmarkRoot
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.engine.prompt.ShareData
import mozilla.components.support.ktx.kotlin.isUrl
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.NavGraphDirections
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.BrowserAnimator
import org.mozilla.fenix.browser.BrowserAnimator.Companion.getToolbarNavOptions
import org.mozilla.fenix.browser.BrowserFragmentDirections
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.browser.readermode.ReaderModeController
import org.mozilla.fenix.collections.SaveCollectionStep
import org.mozilla.fenix.components.FenixSnackbar
import org.mozilla.fenix.components.TabCollectionStorage
import org.mozilla.fenix.components.TopSiteStorage
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getRootView
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.navigateSafe
import org.mozilla.fenix.ext.sessionsOfType
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.settings.deletebrowsingdata.deleteAndQuit
import org.mozilla.fenix.utils.Do

/**
 * An interface that handles the view manipulation of the BrowserToolbar, triggered by the Interactor
 */
interface BrowserToolbarController {
    fun handleScroll(offset: Int)
    fun handleToolbarPaste(text: String)
    fun handleToolbarPasteAndGo(text: String)
    fun handleToolbarItemInteraction(item: ToolbarMenu.Item)
    fun handleToolbarClick()
    fun handleTabCounterClick()
    fun handleTabCounterItemInteraction(item: TabCounterMenuItem)
    fun handleBrowserMenuDismissed(lowPrioHighlightItems: List<ToolbarMenu.Item>)
    fun handleReaderModePressed(enabled: Boolean)
}

@Suppress("LargeClass", "TooManyFunctions")
class DefaultBrowserToolbarController(
    private val activity: HomeActivity,
    private val navController: NavController,
    private val readerModeController: ReaderModeController,
    private val sessionManager: SessionManager,
    private val findInPageLauncher: () -> Unit,
    private val engineView: EngineView,
    private val browserAnimator: BrowserAnimator,
    private val swipeRefresh: SwipeRefreshLayout,
    private val customTabSession: Session?,
    private val openInFenixIntent: Intent,
    private val bookmarkTapped: (Session) -> Unit,
    private val scope: CoroutineScope,
    private val tabCollectionStorage: TabCollectionStorage,
    private val topSiteStorage: TopSiteStorage,
    private val onTabCounterClicked: () -> Unit,
    private val onCloseTab: (Session) -> Unit
) : BrowserToolbarController {

    private val currentSession
        get() = customTabSession ?: activity.components.core.sessionManager.selectedSession

    // We hold onto a reference of the inner scope so that we can override this with the
    // TestCoroutineScope to ensure sequential execution. If we didn't have this, our tests
    // would fail intermittently due to the async nature of coroutine scheduling.
    @VisibleForTesting
    internal var ioScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    override fun handleToolbarPaste(text: String) {
        val directions = BrowserFragmentDirections.actionBrowserFragmentToSearchFragment(
            sessionId = currentSession?.id,
            pastedText = text
        )
        navController.nav(R.id.browserFragment, directions, getToolbarNavOptions(activity))
    }

    override fun handleToolbarPasteAndGo(text: String) {
        if (text.isUrl()) {
            sessionManager.selectedSession?.searchTerms = ""
            activity.components.useCases.sessionUseCases.loadUrl.invoke(text)
            return
        }

        sessionManager.selectedSession?.searchTerms = text
        activity.components.useCases.searchUseCases.defaultSearch.invoke(text)
    }

    override fun handleToolbarClick() {
        activity.components.analytics.metrics.track(
            Event.SearchBarTapped(Event.SearchBarTapped.Source.BROWSER)
        )

            val directions = BrowserFragmentDirections.actionBrowserFragmentToSearchFragment(
                currentSession?.id
            )

            navController.nav(R.id.browserFragment, directions, getToolbarNavOptions(activity))
    }

    override fun handleTabCounterClick() {
        onTabCounterClicked.invoke()
    }

    override fun handleReaderModePressed(enabled: Boolean) {
        if (enabled) {
            readerModeController.showReaderView()
            activity.components.analytics.metrics.track(Event.ReaderModeOpened)
        } else {
            readerModeController.hideReaderView()
            activity.components.analytics.metrics.track(Event.ReaderModeClosed)
        }
    }

    override fun handleTabCounterItemInteraction(item: TabCounterMenuItem) {
        when (item) {
            is TabCounterMenuItem.CloseTab -> {
                sessionManager.selectedSession?.let {
                    // When closing the last tab we must show the undo snackbar in the home fragment
                    if (sessionManager.sessionsOfType(it.private).count() == 1) {
                        // The tab tray always returns to normal mode so do that here too
                        activity.browsingModeManager.mode = BrowsingMode.Normal
                        navController.navigate(BrowserFragmentDirections.actionGlobalHome(sessionToDelete = it.id))
                    } else {
                        onCloseTab.invoke(it)
                        activity.components.useCases.tabsUseCases.removeTab.invoke(it)
                    }
                }
            }
            is TabCounterMenuItem.NewTab -> {
                activity.browsingModeManager.mode = BrowsingMode.fromBoolean(item.isPrivate)
                navController.popBackStack(R.id.homeFragment, false)
            }
        }
    }

    override fun handleBrowserMenuDismissed(lowPrioHighlightItems: List<ToolbarMenu.Item>) {
        val settings = activity.settings()
        lowPrioHighlightItems.forEach {
            when (it) {
                ToolbarMenu.Item.AddToHomeScreen -> settings.installPwaOpened = true
                ToolbarMenu.Item.OpenInApp -> settings.openInAppOpened = true
            }
        }
    }

    override fun handleScroll(offset: Int) {
        engineView.setVerticalClipping(offset)
    }

    @ExperimentalCoroutinesApi
    @Suppress("ComplexMethod", "LongMethod")
    override fun handleToolbarItemInteraction(item: ToolbarMenu.Item) {
        val sessionUseCases = activity.components.useCases.sessionUseCases
        trackToolbarItemInteraction(item)

        Do exhaustive when (item) {
            ToolbarMenu.Item.Back -> sessionUseCases.goBack.invoke(currentSession)
            ToolbarMenu.Item.Forward -> sessionUseCases.goForward.invoke(currentSession)
            ToolbarMenu.Item.Reload -> sessionUseCases.reload.invoke(currentSession)
            ToolbarMenu.Item.Stop -> sessionUseCases.stopLoading.invoke(currentSession)
            ToolbarMenu.Item.Settings -> browserAnimator.captureEngineViewAndDrawStatically {
                val directions = BrowserFragmentDirections.actionBrowserFragmentToSettingsFragment()
                navController.nav(R.id.browserFragment, directions)
            }
            ToolbarMenu.Item.SyncedTabs -> {
                navController.nav(
                    R.id.browserFragment,
                    BrowserFragmentDirections.actionBrowserFragmentToSyncedTabsFragment()
                )
            }
            is ToolbarMenu.Item.RequestDesktop -> sessionUseCases.requestDesktopSite.invoke(
                item.isChecked,
                currentSession
            )
            ToolbarMenu.Item.AddToTopSites -> {
                scope.launch {
                    ioScope.launch {
                        currentSession?.let {
                            topSiteStorage.addTopSite(it.title, it.url)
                        }
                    }.join()

                    FenixSnackbar.make(
                        view = swipeRefresh,
                        duration = Snackbar.LENGTH_SHORT,
                        isDisplayedWithBrowserToolbar = true
                    )
                        .setText(
                            swipeRefresh.context.getString(R.string.snackbar_added_to_top_sites)
                        )
                        .show()
                }
            }
            ToolbarMenu.Item.AddToHomeScreen, ToolbarMenu.Item.InstallToHomeScreen -> {
                activity.settings().installPwaOpened = true
                MainScope().launch {
                    with(activity.components.useCases.webAppUseCases) {
                        if (isInstallable()) {
                            addToHomescreen()
                        } else {
                            val directions =
                                BrowserFragmentDirections.actionBrowserFragmentToCreateShortcutFragment()
                            navController.navigateSafe(R.id.browserFragment, directions)
                        }
                    }
                }
            }
            ToolbarMenu.Item.Share -> {
                val directions = NavGraphDirections.actionGlobalShareFragment(
                    data = arrayOf(
                        ShareData(
                            url = currentSession?.url,
                            title = currentSession?.title
                        )
                    ),
                    showPage = true
                )
                navController.navigate(directions)
            }

            ToolbarMenu.Item.FindInPage -> {
                findInPageLauncher()
                activity.components.analytics.metrics.track(Event.FindInPageOpened)
            }

            ToolbarMenu.Item.AddonsManager -> {
                navController.nav(
                    R.id.browserFragment,
                    BrowserFragmentDirections.actionGlobalAddonsManagementFragment()
                )
            }
            ToolbarMenu.Item.SaveToCollection -> {
                activity.components.analytics.metrics
                    .track(Event.CollectionSaveButtonPressed(TELEMETRY_BROWSER_IDENTIFIER))

                currentSession?.let { currentSession ->
                    val directions =
                        BrowserFragmentDirections.actionGlobalCollectionCreationFragment(
                            tabIds = arrayOf(currentSession.id),
                            selectedTabIds = arrayOf(currentSession.id),
                            saveCollectionStep = if (tabCollectionStorage.cachedTabCollections.isEmpty()) {
                                SaveCollectionStep.NameCollection
                            } else {
                                SaveCollectionStep.SelectCollection
                            }
                        )
                    navController.nav(R.id.browserFragment, directions)
                }
            }
            ToolbarMenu.Item.OpenInFenix -> {
                // Release the session from this view so that it can immediately be rendered by a different view
                engineView.release()

                // Strip the CustomTabConfig to turn this Session into a regular tab and then select it
                customTabSession!!.customTabConfig = null
                sessionManager.select(customTabSession)

                // Switch to the actual browser which should now display our new selected session
                activity.startActivity(openInFenixIntent)

                // Close this activity since it is no longer displaying any session
                activity.finish()
            }
            ToolbarMenu.Item.Quit -> {
                // We need to show the snackbar while the browsing data is deleting (if "Delete
                // browsing data on quit" is activated). After the deletion is over, the snackbar
                // is dismissed.
                val snackbar: FenixSnackbar? = activity.getRootView()?.let { v ->
                    FenixSnackbar.make(
                        view = v,
                        duration = Snackbar.LENGTH_LONG,
                        isDisplayedWithBrowserToolbar = true
                    )
                        .setText(v.context.getString(R.string.deleting_browsing_data_in_progress))
                }

                deleteAndQuit(activity, scope, snackbar)
            }
            ToolbarMenu.Item.ReaderModeAppearance -> {
                readerModeController.showControls()
                activity.components.analytics.metrics.track(Event.ReaderModeAppearanceOpened)
            }
            ToolbarMenu.Item.OpenInApp -> {
                activity.settings().openInAppOpened = true

                val appLinksUseCases =
                    activity.components.useCases.appLinksUseCases
                val getRedirect = appLinksUseCases.appLinkRedirect
                currentSession?.let {
                    val redirect = getRedirect.invoke(it.url)
                    redirect.appIntent?.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    appLinksUseCases.openAppLink.invoke(redirect.appIntent)
                }
            }
            ToolbarMenu.Item.Bookmark -> {
                sessionManager.selectedSession?.let {
                    bookmarkTapped(it)
                }
            }
            ToolbarMenu.Item.Bookmarks -> {
                navController.nav(
                    R.id.browserFragment,
                    BrowserFragmentDirections.actionGlobalBookmarkFragment(BookmarkRoot.Mobile.id)
                )
            }
            ToolbarMenu.Item.History -> {
                navController.nav(
                    R.id.browserFragment,
                    BrowserFragmentDirections.actionGlobalHistoryFragment()
                )
            }
        }
    }

    @Suppress("ComplexMethod")
    private fun trackToolbarItemInteraction(item: ToolbarMenu.Item) {
        val eventItem = when (item) {
            ToolbarMenu.Item.Back -> Event.BrowserMenuItemTapped.Item.BACK
            ToolbarMenu.Item.Forward -> Event.BrowserMenuItemTapped.Item.FORWARD
            ToolbarMenu.Item.Reload -> Event.BrowserMenuItemTapped.Item.RELOAD
            ToolbarMenu.Item.Stop -> Event.BrowserMenuItemTapped.Item.STOP
            ToolbarMenu.Item.Settings -> Event.BrowserMenuItemTapped.Item.SETTINGS
            is ToolbarMenu.Item.RequestDesktop ->
                if (item.isChecked) {
                    Event.BrowserMenuItemTapped.Item.DESKTOP_VIEW_ON
                } else {
                    Event.BrowserMenuItemTapped.Item.DESKTOP_VIEW_OFF
                }

            ToolbarMenu.Item.FindInPage -> Event.BrowserMenuItemTapped.Item.FIND_IN_PAGE
            ToolbarMenu.Item.OpenInFenix -> Event.BrowserMenuItemTapped.Item.OPEN_IN_FENIX
            ToolbarMenu.Item.Share -> Event.BrowserMenuItemTapped.Item.SHARE
            ToolbarMenu.Item.SaveToCollection -> Event.BrowserMenuItemTapped.Item.SAVE_TO_COLLECTION
            ToolbarMenu.Item.AddToTopSites -> Event.BrowserMenuItemTapped.Item.ADD_TO_TOP_SITES
            ToolbarMenu.Item.AddToHomeScreen -> Event.BrowserMenuItemTapped.Item.ADD_TO_HOMESCREEN
            ToolbarMenu.Item.SyncedTabs -> Event.BrowserMenuItemTapped.Item.SYNC_TABS
            ToolbarMenu.Item.InstallToHomeScreen -> Event.BrowserMenuItemTapped.Item.ADD_TO_HOMESCREEN
            ToolbarMenu.Item.Quit -> Event.BrowserMenuItemTapped.Item.QUIT
            ToolbarMenu.Item.ReaderModeAppearance ->
                Event.BrowserMenuItemTapped.Item.READER_MODE_APPEARANCE
            ToolbarMenu.Item.OpenInApp -> Event.BrowserMenuItemTapped.Item.OPEN_IN_APP
            ToolbarMenu.Item.Bookmark -> Event.BrowserMenuItemTapped.Item.BOOKMARK
            ToolbarMenu.Item.AddonsManager -> Event.BrowserMenuItemTapped.Item.ADDONS_MANAGER
            ToolbarMenu.Item.Bookmarks -> Event.BrowserMenuItemTapped.Item.BOOKMARKS
            ToolbarMenu.Item.History -> Event.BrowserMenuItemTapped.Item.HISTORY
        }

        activity.components.analytics.metrics.track(Event.BrowserMenuItemTapped(eventItem))
    }

    companion object {
        internal const val TELEMETRY_BROWSER_IDENTIFIER = "browserMenu"
    }
}
