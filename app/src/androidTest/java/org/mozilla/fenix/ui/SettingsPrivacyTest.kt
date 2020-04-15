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
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.HomeActivityTestRule
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.TestHelper
import org.mozilla.fenix.helpers.TestHelper.openAppFromExternalLink
import org.mozilla.fenix.helpers.TestHelper.restartApp
import org.mozilla.fenix.ui.robots.addToHomeScreen
import org.mozilla.fenix.ui.robots.browserScreen
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar

/**
 *  Tests for verifying the main three dot menu options
 *
 */

class SettingsPrivacyTest {
    /* ktlint-disable no-blank-line-before-rbrace */ // This imposes unreadable grouping.

    private val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private lateinit var mockWebServer: MockWebServer
    private val pageShortcutName = "TestShortcut"

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
        // Open 3dot (main) menu
        // Select settings
        // Verify header: "Privacy"

        // Verify item: "Tracking Protection" and default value: "On"
        // Verify item: "Tracking Protection" and default value: "On"

        // Verify item: "Logins"

        // Verify item: "Site Permissions"
        // Click on: "Site permissions"
        // Verify sub-menu items...
        // Verify item: Exceptions
        // Verify item: header: "Permissions"
        // Verify item: "Camera" and default value: "ask to allow"
        // Verify item: "Location" and default value: "ask to allow"
        // Verify item: "Microphone" and default value: "ask to allow"
        // Verify item: "Notification" and default value: "ask to allow"

        // Verify item: "Delete browsing data"
        // Click on: "Delete browsing data"
        // Verify sub-menu items...
        // Verify item: "Open tabs"
        // Verify item" <tab count> tabs
        // Verify item: "Browsing history and site data"
        // Verify item" <address count> addresses
        // Verify item:  "Collections
        // Verify item" <collection count> collections
        // Verify item button: "Delete browsing data"

        // Verify item: "Data collection"
        // Click on: "Data collection"
        // Verify sub-menu items...
        // Verify header: "Usage and technical data"
        // Verify description: "Shares performance, usage, hardware and customization data about your browser with Mozilla to help us make Firefox Preview better"
        // Verify item:  toggle default value: 'on'

        // Verify item: "Privacy notice"
        // Verify item: "Leak Canary" and default toggle value: "Off"

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
            verifyPrivateBrowsingButton()
            verifySitePermissionsButton()
            // drill down on search
            verifyDeleteBrowsingDataButton()
            verifyDeleteBrowsingDataOnQuitButton()
            verifyDataCollectionButton()
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

    @Test
    @Ignore("Passes locally, fails on CI. Fix in https://github.com/mozilla-mobile/fenix/issues/9189")
    fun saveLoginFromPromptTest() {
        val saveLoginTest =
            TestAssetHelper.getSaveLoginAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(saveLoginTest.url) {
            verifySaveLoginPromptIsShown()
            // Click save to save the login
            saveLoginFromPrompt("Save")
        }.openHomeScreen {
        }.openThreeDotMenu {
        }.openSettings {
            TestHelper.scrollToElementByText("Logins and passwords")
        }.openLoginsAndPasswordSubMenu {
            verifyDefaultView()
            verifyDefaultValueSyncLogins()
        }.openSavedLogins {
            verifySavedLoginsView()
            tapSetupLater()
            // Verify that the login appears correctly
            verifySavedLoginFromPrompt()
        }
    }

    @Test
    @Ignore("Passes locally, fails on CI. Fix in https://github.com/mozilla-mobile/fenix/issues/9189")
    fun doNotSaveLoginFromPromptTest() {
        val saveLoginTest = TestAssetHelper.getSaveLoginAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(saveLoginTest.url) {
            verifySaveLoginPromptIsShown()
            // Don't save the login
            saveLoginFromPrompt("Don’t save")
        }.openHomeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openLoginsAndPasswordSubMenu {
            verifyDefaultView()
            verifyDefaultValueSyncLogins()
        }.openSavedLogins {
            verifySavedLoginsView()
            tapSetupLater()
            // Verify that the login list is empty
            verifyNotSavedLoginFromPromt()
        }
    }

    @Test
    fun saveLoginsAndPasswordsOptions() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openLoginsAndPasswordSubMenu {
        }.saveLoginsAndPasswordsOptions {
            verifySaveLoginsOptionsView()
        }
    }

    @Test
    fun verifyPrivateBrowsingMenuItemsTest() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openPrivateBrowsingSubMenu {
            verifyAddPrivateBrowsingShortcutButton()
            verifyOpenLinksInPrivateTab()
            verifyOpenLinksInPrivateTabOff()
        }.goBack {
            verifySettingsView()
        }
    }

    @Test
    fun openExternalLinksInPrivateTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        setOpenLinksInPrivateOn()

        openAppFromExternalLink(defaultWebPage.url.toString())

        browserScreen {
        }.openHomeScreen {
            verifyPrivateSessionHeader()
        }

        setOpenLinksInPrivateOff()

        openAppFromExternalLink(defaultWebPage.url.toString())

        browserScreen {
        }.openHomeScreen {
            verifyOpenTabsHeader()
        }
    }

    @Test
    fun launchPageShortcutInPrivateModeTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        setOpenLinksInPrivateOn()

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
        }.openAddToHomeScreen {
            addShortcutName(pageShortcutName)
            clickAddShortcutButton()
            clickAddAutomaticallyButton()
        }.openHomeScreenShortcut(pageShortcutName) {
        }.openHomeScreen {
            verifyPrivateSessionHeader()
        }
    }

    @Test
    fun launchLinksInPrivateToggleOffStateDoesntChangeTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)

        setOpenLinksInPrivateOn()

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
        }.openThreeDotMenu {
        }.openAddToHomeScreen {
            addShortcutName(pageShortcutName)
            clickAddShortcutButton()
            clickAddAutomaticallyButton()
        }.openHomeScreenShortcut(pageShortcutName) {
        }.openHomeScreen {}

        setOpenLinksInPrivateOff()
        restartApp(activityTestRule)
        mDevice.waitForIdle()

        addToHomeScreen {
        }.searchAndOpenHomeScreenShortcut(pageShortcutName) {
        }.openHomeScreen {
            verifyOpenTabsHeader()
        }.openThreeDotMenu {
        }.openSettings {
        }.openPrivateBrowsingSubMenu {
            verifyOpenLinksInPrivateTabOff()
        }

    }

    @Test
    fun addPrivateBrowsingShortcut() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openPrivateBrowsingSubMenu {
            addPrivateShortcutToHomescreen()
            verifyPrivateBrowsingShortcutIcon()
        }.openPrivateBrowsingShortcut {
            verifySearchView()
        }.openBrowser {
        }.openHomeScreen {
            verifyPrivateSessionHeader()
        }
    }

    @Test
    fun toggleTrackingProtection() {
        homeScreen {
        }.openSearch {
            typeSearch("https://github.com/rpappalax/testapp")
        }.openBrowser {
            verifyPageContent("testapp")
            verifyTrackingProtection()
        }.openHomeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openEnhancedTrackingProtectionSubMenu {
            verifyEnhancedTrackingProtectionDefaults()
            turnOffEnhancedTrackingProtection()
        }.goBack {
        }.goBack {
        }.openSearch {
            typeSearch("https://github.com/rpappalax/testapp")
        }.openBrowser {
            verifyPageContent("testapp")
            verifyTrackingProtectionOff()
        }
    }

    @Ignore("This is a stub test, ignore for now")
    @Test
    fun verifySitePermissions() {
        // Open 3dot (main) menu
        // Select settings
        // Click on: "Site permissions"
        // Verify sub-menu items...
        // Click on: "Exceptions"
        // Verify: "No site exceptions"
        // TBD: create a site exception
        // TBD: return to this UI and verify

        //
        // Open browser to static test website: https://github.com/rpappalax/testapp
        // Click on "Test site permissions: geolocation"
        // Verify that geolocation permissions dialogue is opened
        // Verify text: "Allow <website URL> to use your geolocation?
        // Verify toggle: 'Remember decision for this site?"
        // Verify button: "Don't Allow"
        // Verify button: "Allow" (default)
        // Select "Remember decision for this site"
        // Refresh page
        // Click on "Test site permissions: geolocation"
        // Verify that geolocation permissions dialogue is not opened
        //
        //
        // Open browser to static test website: https://github.com/rpappalax/testapp
        // Click on "Test site permissions: camera"
        // Verify that camera permissions dialogue is opened
        // Verify text: "Allow <website URL> to use your camera?
        // Verify toggle: 'Remember decision for this site?"
        // Verify button: "Don't Allow"
        // Verify button: "Allow" (default)
        // Select "Remember decision for this site"
        // Refresh page
        // Click on "Test site permissions: camera"
        // Verify that camera permissions dialogue is not opened
        //
        //
        // Open browser to static test website: https://github.com/rpappalax/testapp
        // Click on "Test site permissions: microphone"
        // Verify that microphone permissions dialogue is opened
        // Verify text: "Allow <website URL> to use your microphone?
        // Verify toggle: 'Remember decision for this site?"
        // Verify button: "Don't Allow"
        // Verify button: "Allow" (default)
        // Select "Remember decision for this site"
        // Refresh page
        // Click on "Test site permissions: microphone"
        // Verify that microphone permissions dialogue is not opened
        //
        //
        // Open browser to static test website: https://github.com/rpappalax/testapp
        // Click on "Test site permissions: notifications dialogue"
        // Verify that notifications dialogue permissions dialogue is opened
        // Verify text: "Allow <website URL> to send notifications?
        // Verify toggle: 'Remember decision for this site?"
        // Verify button: "Never"
        // Verify button: "Always" (default)
        // Select "Remember decision for this site"
        // Refresh page
        // Click on "Test site permissions: notifications dialogue"
        // Verify that notifications dialogue permissions dialogue is not opened
        //

        // Open 3dot (main) menu
        // Select settings
        // Click on: "Site permissions"
        // Select: Camera
        // Switch from "ask to allow" (default) to "blocked"
        // Click back arrow
        //
        // Select: Location
        // Switch from "ask to allow" (default) to "blocked"
        // Click back arrow
        //
        // Select: Microphone
        // Switch from "ask to allow" (default) to "blocked"
        // Click back arrow
        //
        // Select: Notification
        // Switch from "ask to allow" (default) to "blocked"
        // Click back arrow
        //

        // Open browser to static test website: https://github.com/rpappalax/testapp
        // Click on "Test site permissions: camera dialogue"
        // Verify that notifications dialogue permissions dialogue is not opened
        //
        // Open browser to static test website: https://github.com/rpappalax/testapp
        // Click on "Test site permissions: geolocation dialogue"
        // Verify that notifications dialogue permissions dialogue is not opened
        //
        // Open browser to static test website: https://github.com/rpappalax/testapp
        // Click on "Test site permissions: microphone dialogue"
        // Verify that notifications dialogue permissions dialogue is not opened
        //
        // Open browser to static test website: https://github.com/rpappalax/testapp
        // Click on "Test site permissions: notifications dialogue"
        // Verify that notifications dialogue permissions dialogue is not opened
    }

    @Test
    fun deleteBrowsingData() {
        val page1: TestAssetHelper.TestAsset = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val page2: TestAssetHelper.TestAsset = TestAssetHelper.getGenericAsset(mockWebServer, 2)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(page1.url) {
            verifyPageContent(page1.content)
        }.openHomeScreen {
        }.openNavigationToolbar {
        }.enterURLAndEnterToBrowser(page2.url) {
            verifyPageContent(page2.content)
        }.openHomeScreen {
            saveToCollection("col 1")
        }.openThreeDotMenu {
        }.openSettings {
        }.openDeleteBrowsingDataSubMenu {
            verifyTabsCount("2")
            verifyAddressesCount("2")
            clickDeleteBrowsingData()
        }.openHomeScreen {
            verifyNoTabsOpenedHeader()
            verifyNoTabsOpenedText()
            verifyCollectionsHeader()
            verifyCollectionTitle("col 1")
        }
    }

    @Test
    fun verifyDataCollection() {
        homeScreen{
        }.openThreeDotMenu {
        }.openSettings {
        }.openDataCollectionSubMenu {
            verifyHeaderUsageAndTechnicalData()
            verifyDescUsageAndTechnicalData()
            verifyUsageDataToggleDefault()
        }
    }

    @Test
    fun openPrivacyNotice() {
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
        }.openAboutFirefoxPreview {
            verifyPrivacyNotice()
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

private fun setOpenLinksInPrivateOn() {
    homeScreen {
    }.openThreeDotMenu {
    }.openSettings {
    }.openPrivateBrowsingSubMenu {
        verifyOpenLinksInPrivateTabEnabled()
        clickOpenLinksInPrivateTabSwitch()
    }.goBack {
    }.goBack {
        verifyHomeComponent()
    }
}

private fun setOpenLinksInPrivateOff() {
    homeScreen {
    }.openThreeDotMenu {
    }.openSettings {
    }.openPrivateBrowsingSubMenu {
        clickOpenLinksInPrivateTabSwitch()
        verifyOpenLinksInPrivateTabOff()
    }.goBack {
    }.goBack {
        verifyHomeComponent()
    }
}
