/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.Constants.defaultTopSitesList
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.TestHelper.exitMenu
import org.mozilla.fenix.helpers.TestHelper.getSponsoredShortcutTitle
import org.mozilla.fenix.ui.robots.homeScreen

/**
 * Tests Sponsored shortcuts functionality
 */

class SponsoredShortcutsTest {
    private lateinit var mDevice: UiDevice
    private lateinit var mockWebServer: MockWebServer
    private val defaultSearchEngine = "Amazon.com"
    private lateinit var sponsoredShortcutTitle: String
    private lateinit var sponsoredShortcutTitle2: String

    @get:Rule
    val activityIntentTestRule = HomeActivityIntentTestRule.withDefaultSettingsOverrides(skipOnboarding = true)

    @Before
    fun setUp() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
            start()
        }
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // Expected for en-us defaults
    @SmokeTest
    @Test
    fun verifySponsoredShortcutsListTest() {
        homeScreen {
            defaultTopSitesList.values.forEach { value ->
                verifyExistingTopSitesTabs(value)
            }
        }.openThreeDotMenu {
        }.openCustomizeHome {
            verifySponsoredShortcutsCheckBox(true)
            clickSponsoredShortcuts()
            verifySponsoredShortcutsCheckBox(false)
        }.goBack {
            verifyNotExistingSponsoredTopSitesList()
        }
    }

    @Test
    fun openSponsoredShortcutTest() {
        homeScreen {
            sponsoredShortcutTitle = getSponsoredShortcutTitle(2)
        }.openSponsoredShortcut(sponsoredShortcutTitle) {
            verifyUrl(sponsoredShortcutTitle)
        }
    }

    @Test
    fun openSponsoredShortcutInPrivateBrowsingTest() {
        homeScreen {
            sponsoredShortcutTitle = getSponsoredShortcutTitle(2)
        }.openContextMenuOnSponsoredShortcut(sponsoredShortcutTitle) {
        }.openTopSiteInPrivateTab {
            verifyUrl(sponsoredShortcutTitle)
        }
    }

    @Ignore("Failing, see: https://github.com/mozilla-mobile/fenix/issues/25926")
    @Test
    fun verifySponsorsAndPrivacyLinkTest() {
        homeScreen {
            sponsoredShortcutTitle = getSponsoredShortcutTitle(2)
        }.openContextMenuOnSponsoredShortcut(sponsoredShortcutTitle) {
        }.clickSponsorsAndPrivacyButton {
            verifyUrl("support.mozilla.org/en-US/kb/sponsor-privacy")
        }
    }

    @Test
    fun verifySponsoredShortcutsSettingsOptionTest() {
        homeScreen {
            sponsoredShortcutTitle = getSponsoredShortcutTitle(2)
        }.openContextMenuOnSponsoredShortcut(sponsoredShortcutTitle) {
        }.clickSponsoredShortcutsSettingsButton {
            verifyHomePageView()
        }
    }

    @Test
    fun verifySponsoredShortcutsDetailsTest() {
        homeScreen {
            sponsoredShortcutTitle = getSponsoredShortcutTitle(2)
            sponsoredShortcutTitle2 = getSponsoredShortcutTitle(3)

            verifySponsoredShortcutDetails(sponsoredShortcutTitle, 2)
            verifySponsoredShortcutDetails(sponsoredShortcutTitle2, 3)
        }
    }

    // The default search engine should not be displayed as a sponsored shortcut
    @Test
    fun defaultSearchEngineIsNotDisplayedAsSponsoredShortcutTest() {
        val sponsoredShortcutTitle = "Amazon"

        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openSearchSubMenu {
            changeDefaultSearchEngine(defaultSearchEngine)
        }

        exitMenu()

        homeScreen {
            verifySponsoredShortcutDoesNotExist(sponsoredShortcutTitle, 2)
            verifySponsoredShortcutDoesNotExist(sponsoredShortcutTitle, 3)
        }
    }

    // 1 sponsored shortcut should be displayed if there are 7 pinned top sites
    @Test
    fun verifySponsoredShortcutsListWithSevenPinnedSitesTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val secondWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 2)
        val thirdWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 3)
        val fourthWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 4)

        homeScreen {
            sponsoredShortcutTitle = getSponsoredShortcutTitle(2)
            sponsoredShortcutTitle2 = getSponsoredShortcutTitle(3)

            verifySponsoredShortcutDetails(sponsoredShortcutTitle, 2)
            verifySponsoredShortcutDetails(sponsoredShortcutTitle2, 3)
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            verifyPageContent(firstWebPage.content)
        }.openThreeDotMenu {
            expandMenu()
        }.addToFirefoxHome {
        }.goToHomescreen {
            verifyExistingTopSitesTabs(firstWebPage.title)
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(secondWebPage.url) {
            verifyPageContent(secondWebPage.content)
        }.openThreeDotMenu {
            expandMenu()
        }.addToFirefoxHome {
        }.goToHomescreen {
            verifyExistingTopSitesTabs(secondWebPage.title)
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(thirdWebPage.url) {
            verifyPageContent(thirdWebPage.content)
        }.openThreeDotMenu {
            expandMenu()
        }.addToFirefoxHome {
        }.goToHomescreen {
            verifyExistingTopSitesTabs(thirdWebPage.title)
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(fourthWebPage.url) {
            verifyPageContent(fourthWebPage.content)
        }.openThreeDotMenu {
            expandMenu()
        }.addToFirefoxHome {
        }.goToHomescreen {
            verifySponsoredShortcutDetails(sponsoredShortcutTitle, 2)
            verifySponsoredShortcutDoesNotExist(sponsoredShortcutTitle2, 3)
        }
    }

    // No sponsored shortcuts should be displayed if there are 8 pinned top sites
    @Test
    fun verifySponsoredShortcutsListWithEightPinnedSitesTest() {
        val firstWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val secondWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 2)
        val thirdWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 3)
        val fourthWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 4)
        val fifthWebPage = TestAssetHelper.getLoremIpsumAsset(mockWebServer)

        homeScreen {
            sponsoredShortcutTitle = getSponsoredShortcutTitle(2)
            sponsoredShortcutTitle2 = getSponsoredShortcutTitle(3)

            verifySponsoredShortcutDetails(sponsoredShortcutTitle, 2)
            verifySponsoredShortcutDetails(sponsoredShortcutTitle2, 3)
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(firstWebPage.url) {
            verifyPageContent(firstWebPage.content)
        }.openThreeDotMenu {
            expandMenu()
        }.addToFirefoxHome {
        }.goToHomescreen {
            verifyExistingTopSitesTabs(firstWebPage.title)
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(secondWebPage.url) {
            verifyPageContent(secondWebPage.content)
        }.openThreeDotMenu {
            expandMenu()
        }.addToFirefoxHome {
        }.goToHomescreen {
            verifyExistingTopSitesTabs(secondWebPage.title)
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(thirdWebPage.url) {
            verifyPageContent(thirdWebPage.content)
        }.openThreeDotMenu {
            expandMenu()
        }.addToFirefoxHome {
        }.goToHomescreen {
            verifyExistingTopSitesTabs(thirdWebPage.title)
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(fourthWebPage.url) {
            verifyPageContent(fourthWebPage.content)
        }.openThreeDotMenu {
            expandMenu()
        }.addToFirefoxHome {
        }.goToHomescreen {
            verifyExistingTopSitesTabs(fourthWebPage.title)
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(fifthWebPage.url) {
            verifyPageContent(fifthWebPage.content)
        }.openThreeDotMenu {
            expandMenu()
        }.addToFirefoxHome {
        }.goToHomescreen {
            verifySponsoredShortcutDoesNotExist(sponsoredShortcutTitle, 2)
            verifySponsoredShortcutDoesNotExist(sponsoredShortcutTitle2, 3)
        }
    }
}
