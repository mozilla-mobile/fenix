/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import android.content.pm.ActivityInfo
import androidx.core.net.toUri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.HomeActivityTestRule
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar
import org.mozilla.fenix.ui.robots.openSaveToCollection

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
    fun verifyPageMainMenuItemsListInPortraitNormalModeTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val youtubeUrl = "www.youtube.com"
        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
            verifyThreeDotMainMenuItems()
            // addOns()
            /*}.clickAddOnsReportSiteIssue {
            verifyUrl("https://webcompat.com/issues/new")
        }.openTabDrawer {
        }.openTab(defaultWebPage.title){
        }.openThreeDotMenu {*/
        }.openHistory {
            verifyTestPageUrl(defaultWebPage.url)
        }.goBackToBrowser {
        }.openThreeDotMenu {
        }.openBookmarks {
            verifyBookmarksMenuView()
            verifyEmptyBookmarksList()
        }.goBackToBrowser {
        }.openThreeDotMenu {
        }.openSyncedTabs {
            verifyNavigationToolBarHeader()
            verifySyncedTabsStatus()
        }.goBack {
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
            openSaveToCollection {
                verifyCollectionNameTextField()
            }
        }.goBack {
        }.openThreeDotMenu {
        }.bookmarkPage {
            verifySnackBarText("Bookmark saved!")
        }.openThreeDotMenu {
        }.sharePage {
            verifyShareAppsLayout()
        }.closeShareDialogReturnToPage {
        }.openThreeDotMenu {
        }.refreshPage {
            verifyUrl(defaultWebPage.url.toString())
        }.openTabDrawer {
            closeTabViaXButton(defaultWebPage.title)
        }.openHomeScreen {
            navigationToolbar {
            }.enterURLAndEnterToBrowser(youtubeUrl.toUri()) {
                verifyBlueDot()
            }.openThreeDotMenu {
                verifyOpenInApp()
            }
        }
    }

    @Test
    fun verifyPageMainMenuItemsListInPortraitPrivateModeTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val youtubeUrl = "www.youtube.com"
        homeScreen {
            togglePrivateBrowsingModeOnOff()
            navigationToolbar {
                navigationToolbar {
                }.enterURLAndEnterToBrowser(defaultWebPage.url) {
                }.openThreeDotMenu {
                    verifyThreeDotMainMenuItems()
                }.openHistory {
                    verifyEmptyHistoryView()
                }.goBackToBrowser {
                }.openThreeDotMenu {
                }.openBookmarks {
                    verifyBookmarksMenuView()
                    verifyEmptyBookmarksList()
                }.goBackToBrowser {
                }.openThreeDotMenu {
                }.openSyncedTabs {
                    verifyNavigationToolBarHeader()
                    verifySyncedTabsStatus()
                }.goBack {
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
                    togglePrivateBrowsingModeOnOff()
                    verifyExistingTopSitesTabs(defaultWebPage.title)
                    togglePrivateBrowsingModeOnOff()
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
                }.sharePage {
                    verifyShareAppsLayout()
                }.closeShareDialogReturnToPage {
                }.openThreeDotMenu {
                }.refreshPage {
                    verifyUrl(defaultWebPage.url.toString())
                }.openTabDrawer {
                    closeTabViaXButton(defaultWebPage.title)
                }.openHomeScreen {
                    navigationToolbar {
                    }.enterURLAndEnterToBrowser(youtubeUrl.toUri()) {
                        verifyBlueDot()
                    }.openThreeDotMenu {
                        verifyOpenInApp()
                    }
                }
            }
        }
    }

    @Test
    fun verifyPageMainMenuItemsListInLandscapeNormalModeTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val youtubeUrl = "www.youtube.com"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        }.openThreeDotMenu {
            // verifyThreeDotMainMenuItems()
        }.openHistory {
            // verifyTestPageUrl(defaultWebPage.url)
        }.goBackToBrowser {
        }.openThreeDotMenu {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        }.openBookmarks {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            verifyBookmarksMenuView()
            verifyEmptyBookmarksList()
        }.goBackToBrowser {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        }.openThreeDotMenu {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        }.openSyncedTabs {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            verifyNavigationToolBarHeader()
            verifySyncedTabsStatus()
        }.goBack {
        }.openThreeDotMenu {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        }.openSettings {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            // verifySettingsView()
        }.goBackToBrowser {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        }.openThreeDotMenu {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            /* }.openFindInPage {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                verifyFindInPageSearchBarItems()
            }.closeFindInPage {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            }.openThreeDotMenu {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) */
        }.addToFirefoxHome {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            verifySnackBarText("Added to top sites!")
        }.openTabDrawer {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        }.openTab(defaultWebPage.title) {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        }.openThreeDotMenu {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        }.openAddToHomeScreen {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            verifyShortcutNameField(defaultWebPage.title)
            clickAddShortcutButton()
            clickAddAutomaticallyButton()
            verifyShortcutIcon()
        }.openHomeScreenShortcut(defaultWebPage.title) {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        }.openThreeDotMenu {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            openSaveToCollection {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                verifyCollectionNameTextField()
            }
        }.goBack {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        }.openThreeDotMenu {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        }.bookmarkPage {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            verifySnackBarText("Bookmark saved!")
        }.openThreeDotMenu {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        }.sharePage {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            verifyShareAppsLayout()
        }.closeShareDialogReturnToPage {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        }.openThreeDotMenu {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        }.refreshPage {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            verifyUrl(defaultWebPage.url.toString())
        }.openTabDrawer {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            closeTabViaXButton(defaultWebPage.title)
        }.openHomeScreen {
            activityTestRule.getActivity()
                .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            navigationToolbar {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            }.enterURLAndEnterToBrowser(youtubeUrl.toUri()) {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                verifyBlueDot()
            }.openThreeDotMenu {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                verifyOpenInApp()
            }
        }
    }

    @Test
    fun verifyPageMainMenuItemsListInLandscapePrivateModeTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val youtubeUrl = "www.youtube.com"

        homeScreen {
            togglePrivateBrowsingModeOnOff()
            navigationToolbar {
            }.enterURLAndEnterToBrowser(defaultWebPage.url) {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            }.openThreeDotMenu {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                // verifyThreeDotMainMenuItems()
            }.openHistory {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                // verifyTestPageUrl(defaultWebPage.url)
            }.goBackToBrowser {
            }.openThreeDotMenu {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            }.openBookmarks {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                verifyBookmarksMenuView()
                verifyEmptyBookmarksList()
            }.goBackToBrowser {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            }.openThreeDotMenu {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            }.openSyncedTabs {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                verifyNavigationToolBarHeader()
                verifySyncedTabsStatus()
            }.goBack {
            }.openThreeDotMenu {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            }.openSettings {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                // verifySettingsView()
            }.goBackToBrowser {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            }.openThreeDotMenu {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                /* }.openFindInPage {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                verifyFindInPageSearchBarItems()
            }.closeFindInPage {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            }.openThreeDotMenu {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) */
            }.addToFirefoxHome {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                verifySnackBarText("Added to top sites!")
            }.openTabDrawer {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            }.openTab(defaultWebPage.title) {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            }.openThreeDotMenu {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            }.openAddToHomeScreen {
                verifyShortcutNameField(defaultWebPage.title)
                clickAddShortcutButton()
                clickAddAutomaticallyButton()
                verifyShortcutIcon()
            }.openHomeScreenShortcut(defaultWebPage.title) {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            }.openThreeDotMenu {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            }.bookmarkPage {
                verifySnackBarText("Bookmark saved!")
            }.openThreeDotMenu {
            }.sharePage {
                verifyShareAppsLayout()
            }.closeShareDialogReturnToPage {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            }.openThreeDotMenu {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            }.refreshPage {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                verifyUrl(defaultWebPage.url.toString())
            }.openTabDrawer {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                closeTabViaXButton(defaultWebPage.title)
            }.openHomeScreen {
                activityTestRule.getActivity()
                    .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                navigationToolbar {
                    activityTestRule.getActivity()
                        .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                }.enterURLAndEnterToBrowser(youtubeUrl.toUri()) {
                    activityTestRule.getActivity()
                        .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                    verifyBlueDot()
                }.openThreeDotMenu {
                    activityTestRule.getActivity()
                        .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                    verifyOpenInApp()
                }
            }
        }
    }
}
