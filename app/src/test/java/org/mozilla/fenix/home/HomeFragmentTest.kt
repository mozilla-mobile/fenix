/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home

import android.content.Context
import android.view.View
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import mozilla.components.browser.menu.view.MenuButton
import mozilla.components.browser.state.selector.findTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.SearchState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.state.selectedOrDefaultSearchEngine
import mozilla.components.feature.top.sites.TopSite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mozilla.fenix.FenixApplication
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.components.Core
import org.mozilla.fenix.ext.application
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.home.HomeFragment.Companion.ALL_NORMAL_TABS
import org.mozilla.fenix.home.HomeFragment.Companion.ALL_PRIVATE_TABS
import org.mozilla.fenix.home.HomeFragment.Companion.AMAZON_SPONSORED_TITLE
import org.mozilla.fenix.home.HomeFragment.Companion.EBAY_SPONSORED_TITLE
import org.mozilla.fenix.utils.Settings
import org.mozilla.fenix.utils.UndoCloseTabSnackBar

class HomeFragmentTest {

    private lateinit var settings: Settings
    private lateinit var context: Context
    private lateinit var core: Core
    private lateinit var homeFragment: HomeFragment

    @Before
    fun setup() {
        settings = mockk(relaxed = true)
        context = mockk(relaxed = true)
        core = mockk(relaxed = true)

        val fenixApplication: FenixApplication = mockk(relaxed = true)

        homeFragment = spyk(HomeFragment())

        every { context.application } returns fenixApplication
        every { homeFragment.context } answers { context }
        every { context.components.settings } answers { settings }
        every { context.components.core } answers { core }
    }

    @Test
    fun `WHEN getTopSitesConfig is called THEN it returns TopSitesConfig with non-null frecencyConfig`() {
        every { settings.topSitesMaxLimit } returns 10

        val topSitesConfig = homeFragment.getTopSitesConfig()

        assertNotNull(topSitesConfig.frecencyConfig)
    }

    @Test
    fun `GIVEN a topSitesMaxLimit WHEN getTopSitesConfig is called THEN it returns TopSitesConfig with totalSites = topSitesMaxLimit`() {
        val topSitesMaxLimit = 10
        every { settings.topSitesMaxLimit } returns topSitesMaxLimit

        val topSitesConfig = homeFragment.getTopSitesConfig()

        assertEquals(topSitesMaxLimit, topSitesConfig.totalSites)
    }

    @Test
    fun `GIVEN the selected search engine is set to eBay WHEN getTopSitesConfig is called THEN providerFilter filters the eBay provided top sites`() {
        mockkStatic("mozilla.components.browser.state.state.SearchStateKt")
        every { core.store } returns mockk() {
            every { state } returns mockk() {
                every { search } returns mockk()
            }
        }
        every { any<SearchState>().selectedOrDefaultSearchEngine } returns mockk {
            every { name } returns EBAY_SPONSORED_TITLE
        }
        val eBayTopSite = TopSite.Provided(1L, EBAY_SPONSORED_TITLE, "eBay.com", "", "", "", 0L)
        val amazonTopSite = TopSite.Provided(2L, AMAZON_SPONSORED_TITLE, "Amazon.com", "", "", "", 0L)
        val firefoxTopSite = TopSite.Provided(3L, "Firefox", "mozilla.org", "", "", "", 0L)
        val providedTopSites = listOf(eBayTopSite, amazonTopSite, firefoxTopSite)

        val topSitesConfig = homeFragment.getTopSitesConfig()

        val filteredProvidedSites = providedTopSites.filter {
            topSitesConfig.providerConfig?.providerFilter?.invoke(it) ?: true
        }
        assertTrue(filteredProvidedSites.containsAll(listOf(amazonTopSite, firefoxTopSite)))
        assertFalse(filteredProvidedSites.contains(eBayTopSite))
    }

    @Test
    fun `WHEN configuration changed menu is dismissed`() {
        val menuButton: MenuButton = mockk(relaxed = true)
        homeFragment.getMenuButton = { menuButton }
        homeFragment.onConfigurationChanged(mockk(relaxed = true))

        verify(exactly = 1) { menuButton.dismissMenu() }
    }

    @Test
    fun `GIVEN the user is in normal mode WHEN checking if should enable wallpaper THEN return true`() {
        val activity: HomeActivity = mockk {
            every { themeManager.currentTheme.isPrivate } returns false
        }
        every { homeFragment.activity } returns activity

        assertTrue(homeFragment.shouldEnableWallpaper())
    }

    @Test
    fun `GIVEN the user is in private mode WHEN checking if should enable wallpaper THEN return false`() {
        val activity: HomeActivity = mockk {
            every { themeManager.currentTheme.isPrivate } returns true
        }
        every { homeFragment.activity } returns activity

        assertFalse(homeFragment.shouldEnableWallpaper())
    }

    private fun testCloseTabSnackBar(
        homeFragment: HomeFragment,
        isPrivate: Boolean,
        multiple: Boolean,
        testFun: (String) -> Unit,
    ) {
        mockkObject(UndoCloseTabSnackBar)

        val anchorView: View = mockk(relaxed = true)
        every { homeFragment.snackbarAnchorView } returns anchorView

        every {
            UndoCloseTabSnackBar.show(
                fragment = homeFragment,
                isPrivate = any(),
                multiple = any(),
                onCancel = any(),
                anchorView = anchorView,
            )
        } just Runs

        val code = if (isPrivate) {
            ALL_PRIVATE_TABS
        } else {
            ALL_NORMAL_TABS
        }
        testFun.invoke(code)

        verify {
            UndoCloseTabSnackBar.show(
                fragment = homeFragment,
                isPrivate = isPrivate,
                multiple = multiple,
                onCancel = any(),
                anchorView = anchorView,
            )
        }
    }

    @Test
    fun `GIVEN normal tabs to remove WHEN removing tabs THEN tabs are removed and proper snackbar is shown`() {
        every { homeFragment.requireComponents.useCases.tabsUseCases.removeNormalTabs() } just Runs
        testCloseTabSnackBar(
            homeFragment = homeFragment,
            multiple = true,
            isPrivate = false,
        ) { sessionCode ->
            homeFragment.removeAllTabsAndShowSnackbar(sessionCode)
            verify { homeFragment.requireComponents.useCases.tabsUseCases.removeNormalTabs() }
        }
    }

    @Test
    fun `GIVEN private tabs to remove WHEN removing tabs THEN tabs are removed and proper snackbar is shown`() {
        every { homeFragment.requireComponents.useCases.tabsUseCases.removePrivateTabs() } just Runs
        testCloseTabSnackBar(
            homeFragment = homeFragment,
            multiple = true,
            isPrivate = true,
        ) { sessionCode ->
            homeFragment.removeAllTabsAndShowSnackbar(sessionCode)
            verify { homeFragment.requireComponents.useCases.tabsUseCases.removePrivateTabs() }
        }
    }

    private fun setupRemoveTabAndShowSnackbar(homeFragment: HomeFragment, isPrivate: Boolean) {
        mockkStatic(BrowserState::findTab)

        every { homeFragment.store.state } returns mockk()
        every { homeFragment.requireComponents.useCases.tabsUseCases.removeTab(any()) } just Runs
        every { any<BrowserState>().findTab(any()) } returns createTab(url = "", private = isPrivate)
    }

    @Test
    fun `GIVEN normal tab to remove WHEN removing tab THEN tab is removed and proper snackbar is shown`() {
        setupRemoveTabAndShowSnackbar(homeFragment = homeFragment, isPrivate = false)
        testCloseTabSnackBar(
            homeFragment = homeFragment,
            multiple = false,
            isPrivate = false,
        ) { sessionId ->
            homeFragment.removeTabAndShowSnackbar(sessionId)
            verify { homeFragment.requireComponents.useCases.tabsUseCases.removeTab(sessionId) }
        }
    }

    @Test
    fun `GIVEN private tab to remove WHEN removing tab THEN tab is removed and proper snackbar is shown`() {
        setupRemoveTabAndShowSnackbar(homeFragment = homeFragment, isPrivate = true)
        testCloseTabSnackBar(
            homeFragment = homeFragment,
            multiple = false,
            isPrivate = true,
        ) { sessionId ->
            homeFragment.removeTabAndShowSnackbar(sessionId)
            verify { homeFragment.requireComponents.useCases.tabsUseCases.removeTab(sessionId) }
        }
    }
}
