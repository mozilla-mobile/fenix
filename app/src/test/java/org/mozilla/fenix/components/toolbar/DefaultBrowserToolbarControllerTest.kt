/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.EngineView
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import org.junit.Before
import org.junit.Test
import org.mozilla.fenix.BrowsingModeManager
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.IntentReceiverActivity

import org.mozilla.fenix.R
import org.mozilla.fenix.browser.BrowserFragment
import org.mozilla.fenix.browser.BrowserFragmentDirections
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.metrics
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.ext.toTab
import org.mozilla.fenix.home.sessioncontrol.Tab

class DefaultBrowserToolbarControllerTest {

    private var context: Context = mockk()
    private var navController: NavController = mockk()
    private var findInPageLauncher: () -> Unit = mockk()
    private lateinit var controller: DefaultBrowserToolbarController


    @Before
    fun setUp() {
        controller = DefaultBrowserToolbarController(
            context = context,
            navController = navController,
            findInPageLauncher = findInPageLauncher,
            nestedScrollQuickActionView = mockk(),
            engineView = mockk(),
            currentSession = currentSession,
            viewModel = mockk(),
            getSupportUrl = mockk()
        )
    }

    @Test
    fun handleToolbarClick() {
        val currentSession: Session = mockk()
        val navController: NavController = mockk(relaxed = true)
        val context: Context = mockk()
        val metrics: MetricController = mockk(relaxed = true)

        val controller = DefaultBrowserToolbarController(
            context = context,
            navController = navController,
            findInPageLauncher = mockk(),
            nestedScrollQuickActionView = mockk(),
            engineView = mockk(),
            currentSession = currentSession,
            viewModel = mockk(),
            getSupportUrl = mockk()
        )

        every { currentSession.id } returns "1"
        every { context.metrics } returns metrics

        controller.handleToolbarClick()

        verify { metrics.track(Event.SearchBarTapped(Event.SearchBarTapped.Source.BROWSER)) }
        verify { navController.nav(
            R.id.browserFragment,
            BrowserFragmentDirections.actionBrowserFragmentToSearchFragment(currentSession.id)
        )}
    }

    @Test
    fun handleToolbarBackPress() {
        val currentSession: Session = mockk()
        val context: Context = mockk()
        val metrics: MetricController = mockk(relaxed = true)
        val sessionUseCases: SessionUseCases = mockk(relaxed = true)
        val item = ToolbarMenu.Item.Back

        val controller = DefaultBrowserToolbarController(
            context = context,
            navController = mockk(),
            findInPageLauncher = mockk(),
            nestedScrollQuickActionView = mockk(),
            engineView = mockk(),
            currentSession = currentSession,
            viewModel = mockk(),
            getSupportUrl = mockk()
        )

        every { context.metrics } returns metrics
        every { context.components.useCases.sessionUseCases } returns sessionUseCases

        controller.handleToolbarItemInteraction(item)

        verify { metrics.track(Event.BrowserMenuItemTapped(Event.BrowserMenuItemTapped.Item.BACK))}
        verify { sessionUseCases.goBack }
    }

    @Test
    fun handleToolbarForwardPress() {
        val currentSession: Session = mockk()
        val context: Context = mockk()
        val metrics: MetricController = mockk(relaxed = true)
        val sessionUseCases: SessionUseCases = mockk(relaxed = true)
        val item = ToolbarMenu.Item.Forward

        val controller = DefaultBrowserToolbarController(
            context = context,
            navController = mockk(),
            findInPageLauncher = mockk(),
            nestedScrollQuickActionView = mockk(),
            engineView = mockk(),
            currentSession = currentSession,
            viewModel = mockk(),
            getSupportUrl = mockk()
        )

        every { context.metrics } returns metrics
        every { context.components.useCases.sessionUseCases } returns sessionUseCases

        controller.handleToolbarItemInteraction(item)

        verify { metrics.track(Event.BrowserMenuItemTapped(Event.BrowserMenuItemTapped.Item.FORWARD))}
        verify { sessionUseCases.goForward }
    }

    @Test
    fun handleToolbarReloadPress() {
        val currentSession: Session = mockk()
        val context: Context = mockk()
        val metrics: MetricController = mockk(relaxed = true)
        val sessionUseCases: SessionUseCases = mockk(relaxed = true)
        val item = ToolbarMenu.Item.Reload

        val controller = DefaultBrowserToolbarController(
            context = context,
            navController = mockk(),
            findInPageLauncher = mockk(),
            nestedScrollQuickActionView = mockk(),
            engineView = mockk(),
            currentSession = currentSession,
            viewModel = mockk(),
            getSupportUrl = mockk()
        )

        every { context.metrics } returns metrics
        every { context.components.useCases.sessionUseCases } returns sessionUseCases

        controller.handleToolbarItemInteraction(item)

        verify { metrics.track(Event.BrowserMenuItemTapped(Event.BrowserMenuItemTapped.Item.RELOAD))}
        verify { sessionUseCases.reload }
    }

    @Test
    fun handleToolbarStopPress() {
        val currentSession: Session = mockk()
        val context: Context = mockk()
        val metrics: MetricController = mockk(relaxed = true)
        val sessionUseCases: SessionUseCases = mockk(relaxed = true)
        val item = ToolbarMenu.Item.Stop

        val controller = DefaultBrowserToolbarController(
            context = context,
            navController = mockk(),
            findInPageLauncher = mockk(),
            nestedScrollQuickActionView = mockk(),
            engineView = mockk(),
            currentSession = currentSession,
            viewModel = mockk(),
            getSupportUrl = mockk()
        )

        every { context.metrics } returns metrics
        every { context.components.useCases.sessionUseCases } returns sessionUseCases

        controller.handleToolbarItemInteraction(item)

        verify { metrics.track(Event.BrowserMenuItemTapped(Event.BrowserMenuItemTapped.Item.STOP))}
        verify { sessionUseCases.stopLoading }
    }

    @Test
    fun handleToolbarSettingsPress() {
        val currentSession: Session = mockk()
        val context: Context = mockk()
        val navController: NavController = mockk(relaxed = true)
        val metrics: MetricController = mockk(relaxed = true)
        val sessionUseCases: SessionUseCases = mockk(relaxed = true)
        val item = ToolbarMenu.Item.Settings

        val controller = DefaultBrowserToolbarController(
            context = context,
            navController = navController,
            findInPageLauncher = mockk(),
            nestedScrollQuickActionView = mockk(),
            engineView = mockk(),
            currentSession = currentSession,
            viewModel = mockk(),
            getSupportUrl = mockk()
        )

        every { context.metrics } returns metrics
        every { context.components.useCases.sessionUseCases } returns sessionUseCases

        controller.handleToolbarItemInteraction(item)

        verify { metrics.track(Event.BrowserMenuItemTapped(Event.BrowserMenuItemTapped.Item.SETTINGS))}
        verify { navController.nav(
            R.id.settingsFragment,
            BrowserFragmentDirections.actionBrowserFragmentToSettingsFragment()
        )}
    }

    @Test
    fun handleToolbarLibraryPress() {
        val currentSession: Session = mockk()
        val context: Context = mockk()
        val navController: NavController = mockk(relaxed = true)
        val metrics: MetricController = mockk(relaxed = true)
        val sessionUseCases: SessionUseCases = mockk(relaxed = true)
        val item = ToolbarMenu.Item.Library

        val controller = DefaultBrowserToolbarController(
            context = context,
            navController = navController,
            findInPageLauncher = mockk(),
            nestedScrollQuickActionView = mockk(),
            engineView = mockk(),
            currentSession = currentSession,
            viewModel = mockk(),
            getSupportUrl = mockk()
        )

        every { context.metrics } returns metrics
        every { context.components.useCases.sessionUseCases } returns sessionUseCases

        controller.handleToolbarItemInteraction(item)

        verify { metrics.track(Event.BrowserMenuItemTapped(Event.BrowserMenuItemTapped.Item.LIBRARY))}
        verify { navController.nav(
            R.id.libraryFragment,
            BrowserFragmentDirections.actionBrowserFragmentToSettingsFragment()
        )}
    }

    @Test
    fun handleToolbarRequestDesktopOnPress() {
        val currentSession: Session = mockk()
        val context: Context = mockk()
        val navController: NavController = mockk(relaxed = true)
        val metrics: MetricController = mockk(relaxed = true)
        val sessionUseCases: SessionUseCases = mockk(relaxed = true)
        val requestDesktopSiteUseCase: SessionUseCases.RequestDesktopSiteUseCase = mockk(relaxed = true)
        val item = ToolbarMenu.Item.RequestDesktop(true)

        val controller = DefaultBrowserToolbarController(
            context = context,
            navController = navController,
            findInPageLauncher = mockk(),
            nestedScrollQuickActionView = mockk(),
            engineView = mockk(),
            currentSession = currentSession,
            viewModel = mockk(),
            getSupportUrl = mockk()
        )

        every { context.metrics } returns metrics
        every { context.components.useCases.sessionUseCases } returns sessionUseCases
        every { sessionUseCases.requestDesktopSite } returns requestDesktopSiteUseCase

        controller.handleToolbarItemInteraction(item)

        verify { metrics.track(Event.BrowserMenuItemTapped(Event.BrowserMenuItemTapped.Item.DESKTOP_VIEW_ON))}
        verify {
            requestDesktopSiteUseCase.invoke(
                true,
                currentSession
            )
        }
    }

    @Test
    fun handleToolbarRequestDesktopOffPress() {
        val currentSession: Session = mockk()
        val context: Context = mockk()
        val navController: NavController = mockk(relaxed = true)
        val metrics: MetricController = mockk(relaxed = true)
        val sessionUseCases: SessionUseCases = mockk(relaxed = true)
        val requestDesktopSiteUseCase: SessionUseCases.RequestDesktopSiteUseCase = mockk(relaxed = true)
        val item = ToolbarMenu.Item.RequestDesktop(false)

        val controller = DefaultBrowserToolbarController(
            context = context,
            navController = navController,
            findInPageLauncher = mockk(),
            nestedScrollQuickActionView = mockk(),
            engineView = mockk(),
            currentSession = currentSession,
            viewModel = mockk(),
            getSupportUrl = mockk()
        )

        every { context.metrics } returns metrics
        every { context.components.useCases.sessionUseCases } returns sessionUseCases
        every { sessionUseCases.requestDesktopSite } returns requestDesktopSiteUseCase

        controller.handleToolbarItemInteraction(item)

        verify { metrics.track(Event.BrowserMenuItemTapped(Event.BrowserMenuItemTapped.Item.DESKTOP_VIEW_OFF))}
        verify {
            requestDesktopSiteUseCase.invoke(
                false,
                currentSession
            )
        }
    }

    @Test
    fun handleToolbarSharePress() {
        val currentSession: Session = mockk()
        val context: Context = mockk()
        val navController: NavController = mockk(relaxed = true)
        val metrics: MetricController = mockk(relaxed = true)
        val sessionUseCases: SessionUseCases = mockk(relaxed = true)
        val item = ToolbarMenu.Item.Share

        val controller = DefaultBrowserToolbarController(
            context = context,
            navController = navController,
            findInPageLauncher = mockk(),
            nestedScrollQuickActionView = mockk(),
            engineView = mockk(),
            currentSession = currentSession,
            viewModel = mockk(),
            getSupportUrl = mockk()
        )

        every { context.metrics } returns metrics
        every { context.components.useCases.sessionUseCases } returns sessionUseCases
        every { currentSession.url } returns "https://mozilla.org"

        controller.handleToolbarItemInteraction(item)

        verify { metrics.track(Event.BrowserMenuItemTapped(Event.BrowserMenuItemTapped.Item.SHARE))}
        verify {
            currentSession.url.apply {
                val directions = BrowserFragmentDirections.actionBrowserFragmentToShareFragment(this)
                navController.nav(R.id.browserFragment, directions)
            }
        }
    }

    @Test
    fun handleToolbarNewPrivateTabPress() {
        val currentSession: Session = mockk()
        val context: HomeActivity = mockk()
        val browsingModeManager: BrowsingModeManager = mockk(relaxed = true)
        val navController: NavController = mockk(relaxed = true)
        val metrics: MetricController = mockk(relaxed = true)
        val sessionUseCases: SessionUseCases = mockk(relaxed = true)
        val item = ToolbarMenu.Item.NewPrivateTab

        val controller = DefaultBrowserToolbarController(
            context = context,
            navController = navController,
            findInPageLauncher = mockk(),
            nestedScrollQuickActionView = mockk(),
            engineView = mockk(),
            currentSession = currentSession,
            viewModel = mockk(),
            getSupportUrl = mockk()
        )

        every { context.metrics } returns metrics
        every { context.components.useCases.sessionUseCases } returns sessionUseCases
        every { context.browsingModeManager } returns browsingModeManager
        every { browsingModeManager.mode } returns BrowsingModeManager.Mode.Normal

        controller.handleToolbarItemInteraction(item)

        verify { metrics.track(Event.BrowserMenuItemTapped(Event.BrowserMenuItemTapped.Item.NEW_PRIVATE_TAB))}
        verify {
            val directions = BrowserFragmentDirections
                .actionBrowserFragmentToSearchFragment(null)
            navController.nav(R.id.browserFragment, directions)
        }
        verify { browsingModeManager.mode = BrowsingModeManager.Mode.Private}
    }

    @Test
    fun handleToolbarFindInPagePress() {
        // TODO
    }

    @Test
    fun handleToolbarReportIssuePress() {
        val currentSession: Session = mockk()
        val context: HomeActivity = mockk()
        val navController: NavController = mockk(relaxed = true)
        val metrics: MetricController = mockk(relaxed = true)
        val sessionUseCases: SessionUseCases = mockk(relaxed = true)
        val tabsUseCases: TabsUseCases = mockk(relaxed = true)
        val addTabUseCase: TabsUseCases.AddNewTabUseCase = mockk(relaxed = true)

        val item = ToolbarMenu.Item.ReportIssue

        val controller = DefaultBrowserToolbarController(
            context = context,
            navController = navController,
            findInPageLauncher = mockk(),
            nestedScrollQuickActionView = mockk(),
            engineView = mockk(),
            currentSession = currentSession,
            viewModel = mockk(),
            getSupportUrl = mockk()
        )

        every { context.metrics } returns metrics
        every { context.components.useCases.sessionUseCases } returns sessionUseCases
        every { currentSession.url } returns "https://mozilla.org"
        every { context.components.useCases.tabsUseCases } returns tabsUseCases
        every { tabsUseCases.addTab } returns addTabUseCase

        controller.handleToolbarItemInteraction(item)


        // TODO: Broken
        verify { metrics.track(Event.BrowserMenuItemTapped(Event.BrowserMenuItemTapped.Item.REPORT_SITE_ISSUE))}
        verify {
            currentSession.url.apply {
                val reportUrl = String.format(BrowserFragment.REPORT_SITE_ISSUE_URL, this)
                addTabUseCase.invoke(reportUrl)

            }
        }

        /*
         currentSession.url.apply {
                    val reportUrl = String.format(BrowserFragment.REPORT_SITE_ISSUE_URL, this)
                    context.components.useCases.tabsUseCases.addTab.invoke(reportUrl)
                }
         */
    }

    @Test
    fun handleToolbarHelpPress() {
        val currentSession: Session = mockk()
        val context: HomeActivity = mockk()
        val navController: NavController = mockk(relaxed = true)
        val metrics: MetricController = mockk(relaxed = true)
        val sessionUseCases: SessionUseCases = mockk(relaxed = true)
        val tabsUseCases: TabsUseCases = mockk(relaxed = true)
        val addTabUseCase: TabsUseCases.AddNewTabUseCase = mockk(relaxed = true)
        val getSupportUrl: () -> String = { "https://supportUrl.org" }

        val item = ToolbarMenu.Item.Help

        val controller = DefaultBrowserToolbarController(
            context = context,
            navController = navController,
            findInPageLauncher = mockk(),
            nestedScrollQuickActionView = mockk(),
            engineView = mockk(),
            currentSession = currentSession,
            viewModel = mockk(),
            getSupportUrl = getSupportUrl
        )

        every { context.metrics } returns metrics
        every { context.components.useCases.sessionUseCases } returns sessionUseCases
        every { context.components.useCases.tabsUseCases } returns tabsUseCases
        every { tabsUseCases.addTab } returns addTabUseCase

        controller.handleToolbarItemInteraction(item)

        verify { metrics.track(Event.BrowserMenuItemTapped(Event.BrowserMenuItemTapped.Item.HELP))}
        verify {
            addTabUseCase.invoke(getSupportUrl())
        }
    }

    @Test
    fun handleToolbarNewTabPress() {
        val currentSession: Session = mockk()
        val context: HomeActivity = mockk()
        val browsingModeManager: BrowsingModeManager = mockk(relaxed = true)
        val navController: NavController = mockk(relaxed = true)
        val metrics: MetricController = mockk(relaxed = true)
        val sessionUseCases: SessionUseCases = mockk(relaxed = true)
        val item = ToolbarMenu.Item.NewTab

        val controller = DefaultBrowserToolbarController(
            context = context,
            navController = navController,
            findInPageLauncher = mockk(),
            nestedScrollQuickActionView = mockk(),
            engineView = mockk(),
            currentSession = currentSession,
            viewModel = mockk(),
            getSupportUrl = mockk()
        )

        every { context.metrics } returns metrics
        every { context.components.useCases.sessionUseCases } returns sessionUseCases
        every { context.browsingModeManager } returns browsingModeManager
        every { browsingModeManager.mode } returns BrowsingModeManager.Mode.Private

        controller.handleToolbarItemInteraction(item)

        verify { metrics.track(Event.BrowserMenuItemTapped(Event.BrowserMenuItemTapped.Item.NEW_TAB))}
        verify {
            val directions = BrowserFragmentDirections
                .actionBrowserFragmentToSearchFragment(null)
            navController.nav(R.id.browserFragment, directions)
        }
        verify { browsingModeManager.mode = BrowsingModeManager.Mode.Normal }
    }

    @Test
    fun handleToolbarSaveToCollectionPress() {
        // TODO: Can't parse URI?
        val currentSession: Session = mockk()
        val context: HomeActivity = mockk()
        val navController: NavController = mockk(relaxed = true)
        val metrics: MetricController = mockk(relaxed = true)
        val sessionUseCases: SessionUseCases = mockk(relaxed = true)
        val tab: Tab = mockk()
        val item = ToolbarMenu.Item.SaveToCollection

        val controller = DefaultBrowserToolbarController(
            context = context,
            navController = navController,
            findInPageLauncher = mockk(),
            nestedScrollQuickActionView = mockk(),
            engineView = mockk(),
            currentSession = currentSession,
            viewModel = mockk(),
            getSupportUrl = mockk()
        )

        every { context.metrics } returns metrics
        every { context.components.useCases.sessionUseCases } returns sessionUseCases
        /*
        every { currentSession.url } returns "https://mozilla.org"
        every { currentSession.id } returns "1"
        */

        every { Uri.parse(any()) } returns mockk()
        every { currentSession.toTab(any(), any()) } returns tab

        controller.handleToolbarItemInteraction(item)

        verify { metrics.track(Event.CollectionSaveButtonPressed(DefaultBrowserToolbarController.TELEMETRY_BROWSER_IDENTIFIER))}
        verify {
            val directions = BrowserFragmentDirections
                .actionBrowserFragmentToSearchFragment(null)
            navController.nav(R.id.browserFragment, directions)
        }

        /*
   context.components.analytics.metrics
                   .track(Event.CollectionSaveButtonPressed(TELEMETRY_BROWSER_IDENITIFIER))
               currentSession.let {
                   val tab = it.toTab(context)
                   viewModel.tabs = listOf(tab)
                   val selectedSet = mutableSetOf(tab)
                   viewModel.selectedTabs = selectedSet
                   viewModel.tabCollections =
                       context.components.core.tabCollectionStorage.cachedTabCollections.reversed()
                   viewModel.saveCollectionStep = viewModel.tabCollections.getStepForCollectionsSize()
                   viewModel.snackbarAnchorView = nestedScrollQuickActionView
                   viewModel.previousFragmentId = R.id.browserFragment

                   val directions = BrowserFragmentDirections.actionBrowserFragmentToCreateCollectionFragment()
                   navController.nav(R.id.browserFragment, directions)
               }
    */
    }



    @Test
    fun handleToolbarOpenInFenixPress() {
        val currentSession: Session = mockk(relaxed = true)
        val context: HomeActivity = mockk(relaxed = true)
        val navController: NavController = mockk(relaxed = true)
        val metrics: MetricController = mockk(relaxed = true)
        val sessionUseCases: SessionUseCases = mockk(relaxed = true)
        val sessionManager: SessionManager = mockk(relaxed = true)
        val engineView: EngineView = mockk(relaxed = true)
        val mockIntent: Intent = mockk(relaxed = true)

        val item = ToolbarMenu.Item.OpenInFenix

        val controller = DefaultBrowserToolbarController(
            context = context,
            navController = navController,
            findInPageLauncher = mockk(),
            nestedScrollQuickActionView = mockk(),
            engineView = engineView,
            currentSession = currentSession,
            viewModel = mockk(),
            getSupportUrl = mockk()
        )

        every { context.metrics } returns metrics
        every { context.components.useCases.sessionUseCases } returns sessionUseCases
        every { context.components.core.sessionManager } returns sessionManager
        every { currentSession.customTabConfig } returns mockk()
        every { context.startActivity(any()) } just Runs
        every { controller.openInFenixIntent.action } returns ""
        every { controller.openInFenixIntent.flags } returns -1

        controller.handleToolbarItemInteraction(item)

        verify { engineView.release() }
        verify { currentSession.customTabConfig = null }
        verify { sessionManager.select(currentSession) }
        verify { mockIntent.action = Intent.ACTION_VIEW }
        verify { mockIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK}
        verify { context.startActivity(mockIntent) }
        verify { context.finish() }
        /*
         // Release the session from this view so that it can immediately be rendered by a different view
                    engineView.release()

                    // Strip the CustomTabConfig to turn this Session into a regular tab and then select it
                    currentSession.customTabConfig = null
                    context.components.core.sessionManager.select(currentSession)

                    // Switch to the actual browser which should now display our new selected session
                    context.startActivity(Intent(context, IntentReceiverActivity::class.java).also {
                        it.action = Intent.ACTION_VIEW
                        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })

                    // Close this activity since it is no longer displaying any session
                    (context as Activity).finish()
         */

    }

    @Test
    fun handleToolbarItemInteraction() {


        /*
        val sessionUseCases = context.components.useCases.sessionUseCases
        trackToolbarItemInteraction(item)

        Do exhaustive when (item) {
            ToolbarMenu.Item.Back -> sessionUseCases.goBack.invoke(currentSession)
            ToolbarMenu.Item.Forward -> sessionUseCases.goForward.invoke(currentSession)
            ToolbarMenu.Item.Reload -> sessionUseCases.reload.invoke(currentSession)
            ToolbarMenu.Item.Stop -> sessionUseCases.stopLoading.invoke(currentSession)
            ToolbarMenu.Item.Settings -> navController.nav(
                R.id.browserFragment,
                BrowserFragmentDirections.actionBrowserFragmentToSettingsFragment()
            )
            ToolbarMenu.Item.Library -> navController.nav(
                R.id.browserFragment,
                BrowserFragmentDirections.actionBrowserFragmentToLibraryFragment()
            )
            is ToolbarMenu.Item.RequestDesktop -> sessionUseCases.requestDesktopSite.invoke(
                item.isChecked,
                currentSession
            )
            ToolbarMenu.Item.Share -> {
                currentSession.url.apply {
                    val directions = BrowserFragmentDirections.actionBrowserFragmentToShareFragment(this)
                    navController.nav(R.id.browserFragment, directions)
                }
            }
            ToolbarMenu.Item.NewPrivateTab -> {
                val directions = BrowserFragmentDirections
                    .actionBrowserFragmentToSearchFragment(null)
                navController.nav(R.id.browserFragment, directions)
                (context as HomeActivity).browsingModeManager.mode = BrowsingModeManager.Mode.Private
            }
            ToolbarMenu.Item.FindInPage -> {
                (BottomSheetBehavior.from(nestedScrollQuickActionView) as QuickActionSheetBehavior).apply {
                    state = BottomSheetBehavior.STATE_COLLAPSED
                }
                findInPageLauncher()
                context.components.analytics.metrics.track(Event.FindInPageOpened)
            }
            ToolbarMenu.Item.ReportIssue -> {
                currentSession.url.apply {
                    val reportUrl = String.format(BrowserFragment.REPORT_SITE_ISSUE_URL, this)
                    context.components.useCases.tabsUseCases.addTab.invoke(reportUrl)
                }
            }
            ToolbarMenu.Item.Help -> {
                context.components.useCases.tabsUseCases.addTab.invoke(
                    SupportUtils.getSumoURLForTopic(
                        context,
                        SupportUtils.SumoTopic.HELP
                    )
                )
            }
            ToolbarMenu.Item.NewTab -> {
                val directions = BrowserFragmentDirections
                    .actionBrowserFragmentToSearchFragment(null)
                navController.nav(R.id.browserFragment, directions)
                (context as HomeActivity).browsingModeManager.mode =
                    BrowsingModeManager.Mode.Normal
            }
            ToolbarMenu.Item.SaveToCollection -> {
                context.components.analytics.metrics
                    .track(Event.CollectionSaveButtonPressed(TELEMETRY_BROWSER_IDENITIFIER))
                currentSession.let {
                    val tab = it.toTab(context)
                    viewModel.tabs = listOf(tab)
                    val selectedSet = mutableSetOf(tab)
                    viewModel.selectedTabs = selectedSet
                    viewModel.tabCollections =
                        context.components.core.tabCollectionStorage.cachedTabCollections.reversed()
                    viewModel.saveCollectionStep = viewModel.tabCollections.getStepForCollectionsSize()
                    viewModel.snackbarAnchorView = nestedScrollQuickActionView
                    viewModel.previousFragmentId = R.id.browserFragment

                    val directions = BrowserFragmentDirections.actionBrowserFragmentToCreateCollectionFragment()
                    navController.nav(R.id.browserFragment, directions)
                }
            }
            ToolbarMenu.Item.OpenInFenix -> {
                // Release the session from this view so that it can immediately be rendered by a different view
                engineView.release()

                // Strip the CustomTabConfig to turn this Session into a regular tab and then select it
                currentSession.customTabConfig = null
                context.components.core.sessionManager.select(currentSession)

                // Switch to the actual browser which should now display our new selected session
                context.startActivity(Intent(context, IntentReceiverActivity::class.java).also {
                    it.action = Intent.ACTION_VIEW
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })

                // Close this activity since it is no longer displaying any session
                (context as Activity).finish()
            }
        }
         */
    }
}
