/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import android.content.Context
import androidx.navigation.NavController
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mozilla.components.browser.session.Session
import mozilla.components.feature.session.SessionUseCases
import org.junit.Test

import org.mozilla.fenix.R
import org.mozilla.fenix.browser.BrowserFragmentDirections
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.metrics
import org.mozilla.fenix.ext.nav

class DefaultBrowserToolbarControllerTest {

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
            viewModel = mockk()
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
            viewModel = mockk()
        )

        every { context.metrics } returns metrics
        every { context.components.useCases.sessionUseCases } returns sessionUseCases

        controller.handleToolbarItemInteraction(item)

        verify { metrics.track(Event.BrowserMenuItemTapped(Event.BrowserMenuItemTapped.Item.BACK))}
        verify { sessionUseCases.goBack }
    }

    /*
    @Test
    fun handleToolbarBackPress() {

    }
    */

    @Test
    fun handleToolbarItemInteraction() {

        // TODO: How to test this?
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
