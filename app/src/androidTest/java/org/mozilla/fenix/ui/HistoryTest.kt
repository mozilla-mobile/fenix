/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import android.content.Context
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import kotlinx.coroutines.runBlocking
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.HomeActivityTestRule
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.TestHelper.longTapSelectItem
import org.mozilla.fenix.ui.robots.historyMenu
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.multipleSelectionToolbar
import org.mozilla.fenix.ui.robots.navigationToolbar

/**
 *  Tests for verifying basic functionality of history
 *
 */
class HistoryTest {
    /* ktlint-disable no-blank-line-before-rbrace */ // This imposes unreadable grouping.
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
        // Clearing all history data after each test to avoid overlapping data
        val applicationContext: Context = activityTestRule.activity.applicationContext
        val historyStorage = PlacesHistoryStorage(applicationContext)
        runBlocking {
            historyStorage.deleteEverything()
        }
    }

    @Test
    fun noHistoryItemsInCacheTest() {
        homeScreen {
        }.openThreeDotMenu {
            verifyHistoryButton()
        }.openHistory {
            verifyHistoryMenuView()
            verifyEmptyHistoryView()
        }
    }

    @Test
    fun visitedUrlHistoryTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            verifyPageContent("Page content: 1")
        }.openThreeDotMenu {
        }.openLibrary {
        }.openHistory {
            verifyHistoryMenuView()
            verifyVisitedTimeTitle()
            verifyFirstTestPageTitle("Test_Page_1")
            verifyTestPageUrl(firstWebPage.url)
        }
    }

    @Test
    fun deleteHistoryItemTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            verifyPageContent("Page content: 1")
        }.openThreeDotMenu {
        }.openLibrary {
        }.openHistory {
            openOverflowMenu()
            clickThreeDotMenuDelete()
            verifyEmptyHistoryView()
        }
    }

    @Test
    fun deleteAllHistoryTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            verifyPageContent("Page content: 1")
        }.openThreeDotMenu {
        }.openLibrary {
        }.openHistory {
            clickDeleteHistoryButton()
            verifyDeleteConfirmationMessage()
            confirmDeleteAllHistory()
            verifyEmptyHistoryView()
        }
    }

    @Test
    fun multiSelectionToolbarItemsTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            verifyPageContent("Page content: 1")
        }.openThreeDotMenu {
        }.openLibrary {
        }.openHistory {
            longTapSelectItem(firstWebPage.url)
        }

        multipleSelectionToolbar {
            verifyMultiSelectionCheckmark()
            verifyMultiSelectionCounter()
            verifyShareHistoryButton()
            verifyCloseToolbarButton()
        }.closeToolbarReturnToHistory {
            verifyHistoryMenuView()
        }
    }

    @Test
    fun openHistoryInNewTabTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            verifyPageContent("Page content: 1")
        }.openHomeScreen {
            closeTab()
        }.openThreeDotMenu {
        }.openHistory {
            longTapSelectItem(firstWebPage.url)
            openActionBarOverflowOrOptionsMenu(activityTestRule.activity)
        }

        multipleSelectionToolbar {
        }.clickOpenNewTab {
            verifyExistingTabList()
            verifyOpenTabsHeader()
        }
    }

    @Test
    fun openHistoryInPrivateTabTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            verifyPageContent("Page content: 1")
        }.openThreeDotMenu {
        }.openLibrary {
        }.openHistory {
            longTapSelectItem(firstWebPage.url)
            openActionBarOverflowOrOptionsMenu(activityTestRule.activity)
        }

        multipleSelectionToolbar {
        }.clickOpenPrivateTab {
            verifyExistingTabList()
            verifyPrivateSessionHeader()
        }
    }

    @Test
    fun deleteMultipleSelectionTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val secondWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 2)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            verifyPageContent("Page content: 1")
        }.openHomeScreen {}

        navigationToolbar {
        }.enterURLAndEnterToBrowser(secondWebPage.url) {
            verifyPageContent("Page content: 2")
        }.openThreeDotMenu {
        }.openLibrary {
        }.openHistory {
            longTapSelectItem(firstWebPage.url)
            longTapSelectItem(secondWebPage.url)
            openActionBarOverflowOrOptionsMenu(activityTestRule.activity)
        }

        multipleSelectionToolbar {
            clickMultiSelectionDelete()
        }

        historyMenu {
            verifyEmptyHistoryView()
        }
    }

    @Test
    fun shareButtonTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            verifyPageContent("Page content: 1")
        }.openThreeDotMenu {
        }.openLibrary {
        }.openHistory {
            longTapSelectItem(firstWebPage.url)
        }

        multipleSelectionToolbar {
            clickShareHistoryButton()
            verifyShareOverlay()
            verifyShareTabFavicon()
            verifyShareTabTitle()
            verifyShareTabUrl()
        }
    }

    @Test
    fun verifyBackNavigation() {
        homeScreen {
        }.openThreeDotMenu {
        }.openHistory {
        }.goBack {
            verifyHomeScreen()
        }
    }
}
