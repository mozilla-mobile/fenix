/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule
import org.junit.Before
import org.junit.After
import org.junit.Ignore
import org.junit.Test
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.HomeActivityTestRule
import org.mozilla.fenix.helpers.TestAssetHelper.getGenericAsset
import org.mozilla.fenix.helpers.TestAssetHelper.getSitePermissionsAsset
import org.mozilla.fenix.helpers.TestAssetHelper.getVideoAsset
import org.mozilla.fenix.helpers.TestHelper
import org.mozilla.fenix.ui.robots.browserScreen
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar
import org.mozilla.fenix.ui.robots.saveCollection
import org.mozilla.fenix.ui.robots.settingsScreen

/**
 *  Tests for verifying the main three dot menu options
 *
 */

class SettingsPrivacyTest {
    /* ktlint-disable no-blank-line-before-rbrace */ // This imposes unreadable grouping.

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
    // Walks through settings privacy menu and sub-menus to ensure all items are present
    fun settingsPrivacyItemsTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {

            // PRIVACY
            verifyPrivacyHeading()
            verifyEnhancedTrackingProtectionButton()
            verifyEnhancedTrackingProtectionValue("On")
            // Logins
            verifyLoginsButton()
            // drill down to submenu
            verifyAddPrivateBrowsingShortcutButton()
            verifySitePermissionsButton()
            // drill down on search
            verifyDeleteBrowsingDataButton()
            verifyDeleteBrowsingDataOnQuitButton()
            verifyDataCollectionButton()
            verifyLeakCanaryButton()
        }
    }

    // Tests only for initial state without signing in.
    // For tests after singing in, see SyncIntegration test suite

    @Test
    fun loginsAndPasswordsTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
            // Necessary to scroll a little bit for all screen sizes
            TestHelper.scrollToElementByText("Logins and passwords")
        }.openLoginsAndPasswordSubMenu {
            verifyDefaultView()
            verifyDefaultValueSyncLogins()
        }.openSavedLogins {
            verifySavedLoginsView()
            tapSetupLater()
            // Verify that logins list is empty
            // Issue #7272 nothing is shown
        }.goBack {
        }.openSyncLogins {
            verifyReadyToScanOption()
            verifyUseEmailOption()
        }
    }

    // ****
    @Test
    fun verifyCameraSitePermission() {
        val testWebpage = getSitePermissionsAsset(mockWebServer).url

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testWebpage) {
            verifyPageContent(testWebpage.toString())
            pressComponentButton("Camera")
            allowAndroidDialogPrompt()
            allowSitePermission("Camera", true)
        }.openBrowserThreeDotMenu {
        }.refreshPage {
            pressComponentButton("Camera")
            verifyDialogIsNotOpened("Camera")
        }.openThreeDotMenu {
        }.openSettings {
        }.openSitePermissionsSubMenu {
            updatePermissionToBlocked("Camera")
        }.goBack {
        }.goBack {
        }

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testWebpage) {
            pressComponentButton("Camera")
            verifyDialogIsNotOpened("Camera")
        }

    }

    @Test
    fun verifyLocationSitePermission() {
        val testWebpage = getSitePermissionsAsset(mockWebServer).url

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testWebpage) {
            verifyPageContent(testWebpage.toString())
            pressComponentButton("Location")
            allowSitePermission("Location", true)
            allowAndroidDialogPrompt()
        }.openBrowserThreeDotMenu {
        }.refreshPage {
            pressComponentButton("Location")
            verifyDialogIsNotOpened("Location")
        }.openThreeDotMenu {
        }.openSettings {
        }.openSitePermissionsSubMenu {
            updatePermissionToBlocked("Location")
        }.goBack {
        }.goBack {
        }

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testWebpage) {
            pressComponentButton("Location")
            verifyDialogIsNotOpened("Location")
        }
    }

    @Test
    fun verifyMicrophoneSitePermission() {
        val testWebpage = getSitePermissionsAsset(mockWebServer).url

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testWebpage) {
            verifyPageContent(testWebpage.toString())
            pressComponentButton("Microphone")
            allowAndroidDialogPrompt()
            allowSitePermission("Microphone", true)
        }.openBrowserThreeDotMenu {
        }.refreshPage {
            pressComponentButton("Microphone")
            verifyDialogIsNotOpened("Microphone")
        }.openThreeDotMenu {
        }.openSettings {
        }.openSitePermissionsSubMenu {
            updatePermissionToBlocked("Microphone")
        }.goBack {
        }.goBack {
        }

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testWebpage) {
            pressComponentButton("Microphone")
            verifyDialogIsNotOpened("Microphone")
        }
    }

    // ****
    @Test
    fun verifyNotificationSitePermission() {
        val testWebpage = getSitePermissionsAsset(mockWebServer).url

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testWebpage) {
            verifyPageContent(testWebpage.toString())
            pressComponentButton("Notifications")
            allowNotificationSitePermission()
        }.openBrowserThreeDotMenu {
        }.refreshPage {
            pressComponentButton("Notifications")
            verifyDialogIsNotOpened("Notifications")
        }.openThreeDotMenu {
        }.openSettings {
        }.openSitePermissionsSubMenu {
            updatePermissionToBlocked("Notification")
        }.goBack {
        }.goBack {
        }

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testWebpage) {
            pressComponentButton("Notifications")
            verifyDialogIsNotOpened("Notifications")
        }
    }

    @Test
    fun verifyCameraPermissionPrompts() {
        val testWebpage = getSitePermissionsAsset(mockWebServer).url

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testWebpage) {
            verifyPageContent(testWebpage.toString())
            pressComponentButton("Camera")
            allowAndroidDialogPrompt()
            allowSitePermission("Camera")
        }.openBrowserThreeDotMenu {
        }.refreshPage {
            pressComponentButton("Camera")
            denySitePermission("Camera")
        }.openThreeDotMenu {
        }.openSettings {
        }.openSitePermissionsSubMenu {
        }.openExceptions {
            verifyEmptyExceptionList()
        }.goBack {
        }.goBack {
        }.goBack {
        }

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testWebpage) {
            pressComponentButton("Camera")
            denySitePermission("Camera", true)
            Thread.sleep(10000)
        }.openThreeDotMenu {
        }.openSettings {
        }.openSitePermissionsSubMenu {
        }.openExceptions {
            clickExceptionURL("localhost")
            verifySitePermissionRow("Camera", "Blocked")
        }
    }

    @Test
    fun verifyMicrophonePermissionPrompts() {
        val testWebpage = getSitePermissionsAsset(mockWebServer).url

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testWebpage) {
            verifyPageContent(testWebpage.toString())
            pressComponentButton("Microphone")
            allowAndroidDialogPrompt()
            allowSitePermission("Microphone")
        }.openBrowserThreeDotMenu {
        }.refreshPage {
            pressComponentButton("Microphone")
            denySitePermission("Microphone")
        }.openThreeDotMenu {
        }.openSettings {
        }.openSitePermissionsSubMenu {
        }.openExceptions {
            verifyEmptyExceptionList()
        }.goBack {
        }.goBack {
        }.goBack {
        }

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testWebpage) {
            pressComponentButton("Microphone")
            denySitePermission("Microphone", true)
        }.openThreeDotMenu {
        }.openSettings {
        }.openSitePermissionsSubMenu {
        }.openExceptions {
            clickExceptionURL("localhost")
            verifySitePermissionRow("Microphone", "Blocked")
        }
    }

    @Test
    fun verifyLocationPermissionPrompts() {
        val testWebpage = getSitePermissionsAsset(mockWebServer).url

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testWebpage) {
            verifyPageContent(testWebpage.toString())
            pressComponentButton("Location")
            allowSitePermission("Location")
            allowAndroidDialogPrompt()
        }.openBrowserThreeDotMenu {
        }.refreshPage {
            pressComponentButton("Location")
            denySitePermission("Location")
        }.openThreeDotMenu {
        }.openSettings {
        }.openSitePermissionsSubMenu {
        }.openExceptions {
            verifyEmptyExceptionList()
        }.goBack {
        }.goBack {
        }.goBack {
        }

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testWebpage) {
            pressComponentButton("Location")
            denySitePermission("Location", true)
        }.openThreeDotMenu {
        }.openSettings {
        }.openSitePermissionsSubMenu {
        }.openExceptions {
            clickExceptionURL("localhost")
            verifySitePermissionRow("Location", "Blocked")
        }
    }

    // ****
    @Test
    fun verifyNotificationPermissionPrompts() {
        val testWebpage = getSitePermissionsAsset(mockWebServer).url

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testWebpage) {
            verifyPageContent(testWebpage.toString())
            pressComponentButton("Notification")
            allowAndroidDialogPrompt()
            allowNotificationSitePermission()
        }.openBrowserThreeDotMenu {
        }.refreshPage {
            pressComponentButton("Notification")
            denyNotificationSitePermission()
        }.openThreeDotMenu {
        }.openSettings {
        }.openSitePermissionsSubMenu {
        }.openExceptions {
            verifyEmptyExceptionList()
        }.goBack {
        }.goBack {
        }.goBack {
        }

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testWebpage) {
            pressComponentButton("Notification")
            denySitePermission("Notification", true)
        }.openThreeDotMenu {
        }.openSettings {
        }.openSitePermissionsSubMenu {
        }.openExceptions {
            clickExceptionURL("localhost")
            verifySitePermissionRow("Notification", "Blocked")
        }
    }

    @Test
    fun verifyCameraAndMicrophonePermissionPrompts() {
        val testWebpage = getSitePermissionsAsset(mockWebServer).url

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testWebpage) {
            verifyPageContent(testWebpage.toString())
            pressComponentButton("Camera and microphone")
            allowAndroidDialogPrompt() // Allow Video
            allowAndroidDialogPrompt() // Allow Audio
            //website prompt camera and microphone
            allowSitePermission("camera and microphone")
        }.openBrowserThreeDotMenu {
        }.refreshPage {
            pressComponentButton("Camera and microphone")
            denySitePermission("Camera and microphone")
        }.openBrowserThreeDotMenu {
        }.refreshPage {
        }.openThreeDotMenu {
        }.openSettings {
        }.openSitePermissionsSubMenu {
        }.openExceptions {
            verifyEmptyExceptionList()
        }.goBack {
        }.goBack {
        }.goBack {
        }

        browserScreen {
            pressComponentButton("Camera and microphone")
            denySitePermission("Camera and microphone", true)
        }.openThreeDotMenu {
        }.openSettings {
        }.openSitePermissionsSubMenu {
        }.openExceptions {
            clickExceptionURL("localhost")
            verifySitePermissionRow("Camera", "Blocked")
            verifySitePermissionRow("Microphone", "Blocked")
        }
    }

    @Test
    fun verifyDoorhangerPermissions() {
        val testWebpage = getSitePermissionsAsset(mockWebServer).url

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testWebpage) {
            verifyPageContent(testWebpage.toString())
            pressComponentButton("Camera and microphone")
            allowAndroidDialogPrompt() // Allow Video
            allowAndroidDialogPrompt() // Allow Audio
            allowSitePermission("Camera and microphone", true)
        }.openBrowserThreeDotMenu {
        }.refreshPage {
            pressComponentButton("Location")
            allowSitePermission("Location", true)
            allowAndroidDialogPrompt() // Allow Location
        }.openBrowserThreeDotMenu {
        }.refreshPage {
            pressComponentButton("Notification")
            allowNotificationSitePermission()
            verifyPermissionsInDoorhanger()
        }
    }

    @Test
    fun clearAllSiteExceptions() {
        val testWebpage = getSitePermissionsAsset(mockWebServer).url

        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openSitePermissionsSubMenu {
        }.openExceptions {
            verifyEmptyExceptionList()
        }.goBack {
        }.goBack {
        }.goBack {
        }

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testWebpage) {
            verifyPageContent(testWebpage.toString())
            pressComponentButton("Location")
            allowSitePermission("Location", true)
            allowAndroidDialogPrompt()
        }.openBrowserThreeDotMenu {
        }.refreshPage {
            pressComponentButton("Camera")
            allowAndroidDialogPrompt()
            allowSitePermission("Camera", true)
        }.openBrowserThreeDotMenu {
        }.refreshPage {
            pressComponentButton("Microphone")
            allowAndroidDialogPrompt()
            allowSitePermission("Microphone", true)
        }.openThreeDotMenu {
        }.openSettings {
        }.openSitePermissionsSubMenu {
        }.openExceptions {
            verifyExceptionURL("localhost")
            clearAllSitePermissions()
        }.goBack {
        }.goBack {
        }.goBack {
        }

        browserScreen {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(testWebpage) {
            pressComponentButton("Location")
            allowSitePermission("Location", true)
        }.openBrowserThreeDotMenu {
        }.refreshPage {
            pressComponentButton("Camera")
            allowSitePermission("Camera", true)
        }
    }

    // ****
    @Test
    fun clearAllPermissionsOnSite() {
        val testWebpage = getSitePermissionsAsset(mockWebServer).url

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testWebpage) {
            verifyPageContent(testWebpage.toString())
            pressComponentButton("Location")
            allowSitePermission("Location", true)
            allowAndroidDialogPrompt()
        }.openBrowserThreeDotMenu {
        }.refreshPage {
            pressComponentButton("Microphone")
            allowSitePermission("Microphone")
        }.openThreeDotMenu {
        }.openSettings {
        }.openSitePermissionsSubMenu {
        }.openExceptions {
            verifyExceptionURL("localhost")
            clearAllPermissionsOnSite("localhost")
            verifyEmptyExceptionList()
        }.goBack {
        }.goBack {
        }.goBack {
        }

        browserScreen {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(testWebpage) {
            pressComponentButton("Location")
            allowSitePermission("Location", true)
        }.openBrowserThreeDotMenu {
        }.refreshPage {
            pressComponentButton("Microphone")
            allowSitePermission("Microphone")
        }
    }

    // ****
    @Test
    fun clearASingleSitePermission() {
        val testWebpage = getSitePermissionsAsset(mockWebServer).url

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testWebpage) {
            verifyPageContent(testWebpage.toString())
            pressComponentButton("Location")
            allowSitePermission("Location", true)
            allowAndroidDialogPrompt()
        }.openBrowserThreeDotMenu {
        }.refreshPage {
            pressComponentButton("Microphone")
            allowSitePermission("Microphone")
        }.openThreeDotMenu {
        }.openSettings {
        }.openSitePermissionsSubMenu {
        }.openExceptions {
            verifyExceptionURL("localhost")
            clearSinglePermissionOnSite("localhost", "Camera")
        }.goBack {
        }.goBack {
        }.goBack {
        }
    }

    // ****
    @Test
    fun deleteBrowsingData() {
        val page1 = getGenericAsset(mockWebServer, 1)
        val page2 = getGenericAsset(mockWebServer, 2)

        navigationToolbar {
        }.openNewTabAndEnterToBrowser(page1.url) {
        }.openBrowserThreeDotMenu {
        }.openNewTabAndEnterToBrowser(page2.url) {
        }.openHomeScreen {
            saveCollection("Test_Page_1", "Test_Page_2")
        }

        navigationToolbar {
        }.openNewTabAndEnterToBrowser(page1.url) {
        }.openBrowserThreeDotMenu {
        }.openNewTabAndEnterToBrowser(page2.url) {
        }.openHomeScreen {
            saveCollection("Test_Page_1", "Test_Page_2")

        }

        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openDeleteBrowsingDataSubMenu {
            verifyBrowsingData(0, 2, 2)
        }.clearBrowsingData {
        }.goBack {
            verifyNoTabsOpened()
            verifyNoCollectionsOpened()
        }.openThreeDotMenu {
        }.openHistory {
            verifyEmptyHistoryView()
        }
    }

    @Test
    fun deleteOpenTabsBrowsingData() {
        val page1 = getGenericAsset(mockWebServer, 1)
        val page2 = getGenericAsset(mockWebServer, 2)

        navigationToolbar {
        }.openNewTabAndEnterToBrowser(page1.url) {
        }.openBrowserThreeDotMenu {
        }.openNewTabAndEnterToBrowser(page2.url) {
        }.openHomeScreen {
            verifyExistingOpenTabs("Test_Page_1")
            verifyExistingOpenTabs("Test_Page_2")
        }.openThreeDotMenu {
        }.openSettings {
        }.openDeleteBrowsingDataSubMenu {
            deleteOpenTabs()
        }

        // Deleting the Open Tabs takes the user back to the settings screen.
        settingsScreen {
        }.openDeleteBrowsingDataSubMenu {
            verifyZeroOpenTabs()
        }
    }

    @Test
    fun deleteBrowsingHistoryAndSiteBrowsingData() {
        val page1 = getGenericAsset(mockWebServer, 1)
        val page2 = getGenericAsset(mockWebServer, 2)

        navigationToolbar {
        }.openNewTabAndEnterToBrowser(page1.url) {
        }.openBrowserThreeDotMenu {
        }.openNewTabAndEnterToBrowser(page2.url) {
        }.openHomeScreen {
            verifyExistingOpenTabs("Test_Page_1")
            verifyExistingOpenTabs("Test_Page_2")
        }.openThreeDotMenu {
        }.openSettings {
        }.openDeleteBrowsingDataSubMenu {
            deleteBrowsingHistoryAndSiteData()
            verifyBrowsingHistoryAndSiteData()
        }
    }

    @Test
    fun deleteCookiesBrowsingData() {
        val page1 = getGenericAsset(mockWebServer, 1)
        val page2 = getGenericAsset(mockWebServer, 2)

        navigationToolbar {
        }.openNewTabAndEnterToBrowser(page1.url) {
        }.openBrowserThreeDotMenu {
        }.openNewTabAndEnterToBrowser(page2.url) {
        }.openHomeScreen {
            verifyExistingOpenTabs("Test_Page_1")
            verifyExistingOpenTabs("Test_Page_2")
        }.openThreeDotMenu {
        }.openSettings {
        }.openDeleteBrowsingDataSubMenu {
            deleteCookies()
            verifyZeroCookies()
        }
    }

    @Test
    fun deleteCachedImagesAndFilesBrowsingData() {
        val page1 = getGenericAsset(mockWebServer, 1)
        val page2 = getGenericAsset(mockWebServer, 2)

        navigationToolbar {
        }.openNewTabAndEnterToBrowser(page1.url) {
        }.openBrowserThreeDotMenu {
        }.openNewTabAndEnterToBrowser(page2.url) {
        }.openHomeScreen {
            verifyExistingOpenTabs("Test_Page_1")
            verifyExistingOpenTabs("Test_Page_2")
        }.openThreeDotMenu {
        }.openSettings {
        }.openDeleteBrowsingDataSubMenu {
            deleteCachedImagesAndFiles()
            verifyZeroCachedImagesAndFiles()
        }
    }

    @Test
    fun deleteSitePermissionsBrowsingData() {
        val page1 = getGenericAsset(mockWebServer, 1)
        val page2 = getGenericAsset(mockWebServer, 2)

        navigationToolbar {
        }.openNewTabAndEnterToBrowser(page1.url) {
        }.openBrowserThreeDotMenu {
        }.openNewTabAndEnterToBrowser(page2.url) {
        }.openHomeScreen {
            verifyExistingOpenTabs("Test_Page_1")
            verifyExistingOpenTabs("Test_Page_2")
        }.openThreeDotMenu {
        }.openSettings {
        }.openDeleteBrowsingDataSubMenu {
            deleteSitePermissions()
        }.goBack {
        }.openSitePermissionsSubMenu {
        }.openExceptions {
            verifyEmptyExceptionList()
        }
    }

    @Ignore("Autoplay currently doesn't seem to work for some videos, need to investigate. ")
    @Test
    fun verifyAutoplayPermissions() {

        val testWebpage = getVideoAsset(mockWebServer).url


        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openSitePermissionsSubMenu {
            updateAutoplayPermissionToAllowed()
        }.goBack {
        }.goBack {
        }

        navigationToolbar {
        }.enterURLAndEnterToBrowser(testWebpage) {
            verifyPageContent(testWebpage.toString())

            Thread.sleep(5000)
            // Verify if the video autoplayed
        }.openBrowserThreeDotMenu {
        }.clickSettings {
        }.openSitePermissionsSubMenu {
            updateAutoplayPermissionToBlocked()
        }.goBack {
        }.goBack {
        }

        browserScreen {
        }.openBrowserThreeDotMenu {
        }.refreshPage {
            // Verify the video does not autoplay
        }
    }

    @Test
    fun verifyDataCollection() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openDataCollectionSubMenu {
            verifyUsageAndTechnicalDataMenuItem()
        }
    }

    @Test
    fun checkLeakCanary() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
            verifyLeakCanaryButton()
        }
    }
}
