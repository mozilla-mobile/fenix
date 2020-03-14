/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.helpers.AndroidAssetDispatcher
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.ui.robots.navigationToolbar
import org.mozilla.fenix.ui.robots.readerViewRobot

/**
 *  Tests for verifying basic functionality of content context menus
 *
 *  - Verifies Reader View entry and detection when available UI and functionality
 *  - Verifies Reader View exit UI and functionality
 *  - Verifies Reader View appearance controls UI and functionality
 *
 */

class ReaderViewTest {
    private lateinit var mockWebServer: MockWebServer
    val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @get:Rule
    val activityIntentTestRule = HomeActivityIntentTestRule()

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

    /**
     *  Verify that Reader View capable pages
     *
     *   - Show blue notification in the three dot menu
     *   - Show the toggle button in the three dot menu
     *
     */
    @Test
    fun verifyReaderViewPageMenuDetection() {
        val readerViewPage =
            TestAssetHelper.getLoremIpsumAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(readerViewPage.url) {
            verifyPageContent(readerViewPage.content)
        }

        readerViewRobot {
            verifyReaderViewDetected(true)
        }

        navigationToolbar {
        }.openThreeDotMenu {
            verifyReaderViewToggle(true)
        }.closeBrowserMenuToBrowser { }
    }

    /**
     *  Verify that non Reader View capable pages
     *
     *   - Do not show a blue notification in the three dot menu
     *   - Reader View toggle should not be visible in the three dot menu
     *
     */
    @Test
    fun verifyNonReaderViewPageMenuNoDetection() {
        var genericPage =
            TestAssetHelper.getGenericAsset(mockWebServer, 1)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(genericPage.url) {
            verifyPageContent(genericPage.content)
        }

        readerViewRobot {
            verifyReaderViewDetected(false)
        }

        navigationToolbar {
        }.openThreeDotMenu {
            verifyReaderViewToggle(false)
            verifyReaderViewAppearance(false)
        }.closeBrowserMenuToBrowser { }
    }

    @Test
    fun verifyReaderViewToggle() {
        val readerViewPage =
            TestAssetHelper.getLoremIpsumAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(readerViewPage.url) {
            verifyPageContent(readerViewPage.content)
        }

        readerViewRobot {
            verifyReaderViewDetected(true)
        }

        navigationToolbar {
        }.openThreeDotMenu {
            verifyReaderViewToggle(true)
        }.toggleReaderView {
        }.openThreeDotMenu {
            verifyReaderViewAppearance(true)
        }.toggleReaderView {
        }.openThreeDotMenu {
            verifyReaderViewAppearance(false)
        }.close { }

        readerViewRobot {
            verifyReaderViewDetected(false)
        }
    }

    @Test
    fun verifyReaderViewAppearanceUI() {
        val readerViewPage =
            TestAssetHelper.getLoremIpsumAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(readerViewPage.url) {
            verifyPageContent(readerViewPage.content)
        }

        readerViewRobot {
            verifyReaderViewDetected(true)
        }

        navigationToolbar {
        }.openThreeDotMenu {
            verifyReaderViewToggle(true)
        }.toggleReaderView {
        }.openThreeDotMenu {
            verifyReaderViewAppearance(true)
        }.openReaderViewAppearance {
            verifyAppearanceFontGroup(true)
            verifyAppearanceFontSansSerif(true)
            verifyAppearanceFontSerif(true)
            verifyAppearanceFontIncrease(true)
            verifyAppearanceFontDecrease(true)
            verifyAppearanceColorGroup(true)
            verifyAppearanceColorDark(true)
            verifyAppearanceColorLight(true)
            verifyAppearanceColorSepia(true)
        }
    }

    @Test
    fun verifyReaderViewAppearanceFontToggle() {
        val readerViewPage =
            TestAssetHelper.getLoremIpsumAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(readerViewPage.url) {
            verifyPageContent(readerViewPage.content)
        }

        readerViewRobot {
            verifyReaderViewDetected(true)
        }

        navigationToolbar {
        }.openThreeDotMenu {
            verifyReaderViewToggle(true)
        }.toggleReaderView {
        }.openThreeDotMenu {
            verifyReaderViewAppearance(true)
        }.openReaderViewAppearance {
            verifyAppearanceFontGroup(true)
            verifyAppearanceFontSansSerif(true)
            verifyAppearanceFontSerif(true)
            verifyAppearanceFontIncrease(true)
            verifyAppearanceFontDecrease(true)
        }.toggleSansSerif {
            verifyAppearanceFontIsActive("SANSSERIF")
        }.toggleSerif {
            verifyAppearanceFontIsActive("SERIF")
        }
    }

    @Test
    fun verifyReaderViewAppearanceFontSizeToggle() {
        val readerViewPage =
            TestAssetHelper.getLoremIpsumAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(readerViewPage.url) {
            verifyPageContent(readerViewPage.content)
        }

        readerViewRobot {
            verifyReaderViewDetected(true)
        }

        navigationToolbar {
        }.openThreeDotMenu {
            verifyReaderViewToggle(true)
        }.toggleReaderView {
        }.openThreeDotMenu {
            verifyReaderViewAppearance(true)
        }.openReaderViewAppearance {
            verifyAppearanceFontIncrease(true)
            verifyAppearanceFontDecrease(true)
            verifyAppearanceFontSize(3)
        }.toggleFontSizeIncrease {
            verifyAppearanceFontSize(4)
        }.toggleFontSizeIncrease {
            verifyAppearanceFontSize(5)
        }.toggleFontSizeIncrease {
            verifyAppearanceFontSize(6)
        }.toggleFontSizeDecrease {
            verifyAppearanceFontSize(5)
        }.toggleFontSizeDecrease {
            verifyAppearanceFontSize(4)
        }.toggleFontSizeDecrease {
            verifyAppearanceFontSize(3)
        }
    }

    @Test
    fun verifyReaderViewAppearanceColorSchemeChange() {
        val readerViewPage =
            TestAssetHelper.getLoremIpsumAsset(mockWebServer)

        navigationToolbar {
        }.enterURLAndEnterToBrowser(readerViewPage.url) {
            verifyPageContent(readerViewPage.content)
        }

        readerViewRobot {
            verifyReaderViewDetected(true)
        }

        navigationToolbar {
        }.openThreeDotMenu {
            verifyReaderViewToggle(true)
        }.toggleReaderView {
        }.openThreeDotMenu {
            verifyReaderViewAppearance(true)
        }.openReaderViewAppearance {
            verifyAppearanceColorDark(true)
            verifyAppearanceColorLight(true)
            verifyAppearanceColorSepia(true)
        }.toggleColorSchemeChangeDark {
            verifyAppearanceColorSchemeChange("DARK")
        }.toggleColorSchemeChangeSepia {
            verifyAppearanceColorSchemeChange("SEPIA")
        }.toggleColorSchemeChangeLight {
            verifyAppearanceColorSchemeChange("LIGHT")
        }
    }
}
