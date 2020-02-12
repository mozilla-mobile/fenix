package org.mozilla.fenix.ui

import org.mozilla.fenix.helpers.TestAssetHelper

/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

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
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar

/**
 *  Tests for verifying the main three dot menu options
 *
 */

class SettingsAddonsTest {
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
    // Opens a webpage and installs an add-on from the three-dot menu
    fun installAddonFromThreeDotMenu() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 1)
        val addonName = "uBlock Origin"

        navigationToolbar {
        }.openNewTabAndEnterToBrowser(defaultWebPage.url) {
            verifyPageContent(defaultWebPage.content)
        }.openThreeDotMenu {
        }.openAddonsManagerMenu {
            clickInstallAddon(addonName)
            verifyAddonPrompt(addonName)
            cancelInstallAddon()
            clickInstallAddon(addonName)
            acceptInstallAddon()

            verifyDownloadAddonPrompt()
        }
    }

    @Test
    // Walks through settings privacy menu and sub-menus to ensure all items are present
    fun settingsAddonsItemsTest() {
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
            verifyAddPrivateBrowsingShortcutButton()
            verifySitePermissionsButton()
            // drill down on search
            verifyDeleteBrowsingDataButton()
            verifyDeleteBrowsingDataOnQuitButton()
            verifyDataCollectionButton()
            verifyLeakCanaryButton()
        }
    }

}
