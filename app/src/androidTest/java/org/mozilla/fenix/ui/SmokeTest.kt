/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import android.content.pm.ActivityInfo
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.HomeActivityTestRule
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTimeShort
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar

/**
 * Test Suite that contains tests defined as part of the Smoke and Sanity check defined in Test rail.
 * These tests will verify different functionalities of the app as a way to quickly detect regressions in main areas
 */

class SmokeTest {
    private val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private lateinit var mockWebServer: MockWebServer

    @get:Rule
    val activityTestRule = HomeActivityTestRule()

    @Before
    fun setUp() {
        mockWebServer = MockWebServer().apply {
            setDispatcher(AndroidAssetDispatcher())
            start()
        }
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Ignore("Temp disable for triggering a native Gecko crash - https://github.com/mozilla-mobile/fenix/issues/11642")
    @Test
    fun verifyBasicNavigationToolbarFunctionality() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        homeScreen {
            navigationToolbar {
            }.enterURLAndEnterToBrowser(defaultWebPage.url) {
                // verifyPageContent(defaultWebPage.content)
                verifyNavURLBarItems()
            }.openNavigationToolbar {
            }.goBackToWebsite {
                // Check disabled due to intermittent failures
                // verifyPageContent(defaultWebPage.content)
            }.openTabDrawer {
                verifyExistingTabList()
            }.openHomeScreen {
                verifyHomeScreen()
            }
        }
    }

    @Test
    fun verifyPageMainMenuItemsListPotraitMode() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        homeScreen {
            navigationToolbar {
            }.enterURLAndEnterToBrowser(defaultWebPage.url) {
            }.openThreeDotMenu {
                verifyThreeDotMainMenuItems()
            }.openHistory {
                verifyTestPageUrl(defaultWebPage.url)
            }.goBackToBrowser {
            }.openThreeDotMenu {
            }.openBookmarks {
                verifyBookmarksMenuView()
                verifyEmptyBookmarksList()
            }.goBackToBrowser {
            }.openThreeDotMenu {
            }.openSettings {
                verifySettingsView()
            }.goBackToBrowser {
            }.openThreeDotMenu {
            }.openFindInPage {
                verifyFindInPageSearchBarItems()
            }.closeFindInPage {
            }.openThreeDotMenu {
            }.addToFirefoxHome {
                verifySnackBarText("Added to top sites!")
            }.openTabDrawer {
            }.openHomeScreen {
                verifyExistingTopSitesTabs(defaultWebPage.title)
            }.openTabDrawer {
            }.openTab(defaultWebPage.title) {
            }.openThreeDotMenu {
            }.openAddToHomeScreen {
                verifyShortcutNameField(defaultWebPage.title)
                clickAddShortcutButton()
                clickAddAutomaticallyButton()
                verifyShortcutIcon()
            }.openHomeScreenShortcut(defaultWebPage.title) {
            }.openThreeDotMenu {
            }.bookmarkPage {
                verifySnackBarText("Bookmark saved!")
            }.openThreeDotMenu {
                verifyEditBookmarkButton()
            }.openEditBookmarks {
                clickdeleteBookmarkButton()
            }.confirmBookmarkFolderDeletionAndGoBackToBrowser {
                verifySnackBarText("Deleted localhost")
            }.openThreeDotMenu {
            }.sharePage {
                // verifyShareAppsLayout()
                // verifyShareOverlay()
            }.closeShareDialogReturnToPage {
            }.openThreeDotMenu {
            }.refreshPage {
                // verifyPageContent(defaultWebPage.content)
            }.openTabDrawer {
                closeTabViaXButton(defaultWebPage.title)
            }.openHomeScreen {

                // PRIVATE BROWSER

                togglePrivateBrowsingModeOnOff()
                navigationToolbar {
                }.enterURLAndEnterToBrowser(defaultWebPage.url) {
                }.openThreeDotMenu {
                    verifyThreeDotMainMenuItems()
                }.openHistory {
                    verifyTestPageUrl(defaultWebPage.url)
                }.goBackToBrowser {
                }.openThreeDotMenu {
                }.openBookmarks {
                    verifyBookmarksMenuView()
                    // verifyEmptyBookmarksList()
                }.goBackToBrowser {
                }.openThreeDotMenu {
                }.openSettings {
                    verifySettingsView()
                }.goBackToBrowser {
                }.openThreeDotMenu {
                }.openFindInPage {
                    verifyFindInPageSearchBarItems()
                }.closeFindInPage {
                }.openThreeDotMenu {
                }.addToFirefoxHome {
                    verifySnackBarText("Added to top sites!")
                }.openTabDrawer {
                }.openHomeScreen {
                    // verifyExistingTopSitesTabs(defaultWebPage.title)
                }.openTabDrawer {
                }.openTab(defaultWebPage.title) {
                }.openThreeDotMenu {
                }.openAddToHomeScreen {
                    verifyShortcutNameField(defaultWebPage.title)
                    clickAddShortcutButton()
                    clickAddAutomaticallyButton()
                    verifyShortcutIcon()
                }.openHomeScreenShortcut(defaultWebPage.title) {
                }.openThreeDotMenu {
                }.bookmarkPage {
                    verifySnackBarText("Bookmark saved!")
                }.openThreeDotMenu {
                }.openEditBookmarks {
                    clickdeleteBookmarkButton()
                }.confirmBookmarkFolderDeletionAndGoBackToBrowser {
                    verifySnackBarText("Deleted localhost")
                }.openThreeDotMenu {
                }.sharePage {
                    // verifyShareAppsLayout()
                    // verifyShareOverlay()
                }.closeShareDialogReturnToPage {
                }.openThreeDotMenu {
                }.refreshPage {
                    // verifyPageContent(defaultWebPage.content)
                }.openTabDrawer {
                    closeTabViaXButton(defaultWebPage.title)
                }.openHomeScreen {
                }
            }
        }
    }

    @Ignore
    @Test
    fun verifyPageMainMenuItemsListLandscapeMode() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        activityTestRule.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

        homeScreen {
            mDevice.waitForIdle(waitingTimeShort)
            togglePrivateBrowsingModeOnOff()
            navigationToolbar {
            }.enterURLAndEnterToBrowser(defaultWebPage.url) {
                activityTestRule.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            }.openThreeDotMenu {
                verifyThreeDotMainMenuItems()
            }.openHistory {
                // verifyTestPageUrl(defaultWebPage.url)
            }.goBackToBrowser {
            }.openThreeDotMenu {
            }.openBookmarks {
                verifyBookmarksMenuView()
                verifyEmptyBookmarksList()
            }.goBackToBrowser {
            }.openThreeDotMenu {
            }.openSettings {
                verifySettingsView()
            }.goBackToBrowser {
            }.openThreeDotMenu {
            }.openFindInPage {
                verifyFindInPageSearchBarItems()
            }.closeFindInPage {
            }.openThreeDotMenu {
            }.addToFirefoxHome {
                verifySnackBarText("Added to top sites!")
            }.openTabDrawer {
            }.openTab(defaultWebPage.title) {
            }.openThreeDotMenu {
            }.openAddToHomeScreen {
                verifyShortcutNameField(defaultWebPage.title)
                clickAddShortcutButton()
                clickAddAutomaticallyButton()
                verifyShortcutIcon()
            }.openHomeScreenShortcut(defaultWebPage.title) {
            }.openThreeDotMenu {
            }.bookmarkPage {
                verifySnackBarText("Bookmark saved!")
            }.openThreeDotMenu {
                verifyEditBookmarkButton()
            }.openEditBookmarks {
                clickdeleteBookmarkButton()
            }.confirmBookmarkFolderDeletionAndGoBackToBrowser {
                verifySnackBarText("Deleted localhost")
            }.openThreeDotMenu {
            }.sharePage {
                // verifyShareAppsLayout()
                // verifyShareOverlay()
            }.closeShareDialogReturnToPage {
            }.openThreeDotMenu {
            }.refreshPage {
                // verifyPageContent(defaultWebPage.content)
            }.openTabDrawer {
                closeTabViaXButton(defaultWebPage.title)
            }
        }
    }
}
