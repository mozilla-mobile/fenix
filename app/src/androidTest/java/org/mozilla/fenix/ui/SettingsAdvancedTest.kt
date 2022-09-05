/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.Constants.PackageName.GOOGLE_PLAY_SERVICES
import org.mozilla.fenix.helpers.FeatureSettingsHelper
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.TestHelper.assertNativeAppOpens
import org.mozilla.fenix.ui.robots.homeScreen
import org.mozilla.fenix.ui.robots.navigationToolbar

/**
 *  Tests for verifying the advanced section in Settings
 *
 */

class SettingsAdvancedTest {
    /* ktlint-disable no-blank-line-before-rbrace */ // This imposes unreadable grouping.

    private lateinit var mDevice: UiDevice
    private lateinit var mockWebServer: MockWebServer
    private val featureSettingsHelper = FeatureSettingsHelper()

    @get:Rule
    val activityIntentTestRule = HomeActivityIntentTestRule()

    @Before
    fun setUp() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        mockWebServer = MockWebServer().apply {
            dispatcher = AndroidAssetDispatcher()
            start()
        }
        featureSettingsHelper.setPocketEnabled(false)
        featureSettingsHelper.setTCPCFREnabled(false)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()

        featureSettingsHelper.resetAllFeatureFlags()
    }

    @Test
    // Walks through settings menu and sub-menus to ensure all items are present
    fun settingsAboutItemsTest() {
        // ADVANCED
        homeScreen {
        }.openThreeDotMenu {
        }.openSettings {
            // ADVANCED
            verifyAdvancedHeading()
            verifyAddons()
            verifyOpenLinksInAppsButton()
            verifyOpenLinksInAppsSwitchState(false)
            verifyRemoteDebug()
            verifyLeakCanaryButton()
        }
    }

    @SmokeTest
    @Test
    // Assumes Play Store is installed and enabled
    fun openLinkInAppTest() {
        val defaultWebPage = TestAssetHelper.getGenericAsset(mockWebServer, 3)
        val playStoreUrl = "play.google.com/store/apps/details?id=org.mozilla.fenix"
        var context = InstrumentationRegistry.getInstrumentation().targetContext

        homeScreen {
            println("Installed apps:" + context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA))
        }.openThreeDotMenu {
        }.openSettings {
            verifyOpenLinksInAppsSwitchState(false)
            clickOpenLinksInAppsSwitch()
            verifyOpenLinksInAppsSwitchState(true)
        }.goBack {}

        navigationToolbar {
        }.enterURLAndEnterToBrowser(defaultWebPage.url) {
            mDevice.waitForIdle()
            clickLinkMatchingText("Mozilla Playstore link")
            mDevice.waitForIdle()
            assertNativeAppOpens(GOOGLE_PLAY_SERVICES, playStoreUrl)
        }
    }
}
