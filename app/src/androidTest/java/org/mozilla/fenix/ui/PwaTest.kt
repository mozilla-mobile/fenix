package org.mozilla.fenix.ui

import androidx.core.net.toUri
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mozilla.fenix.customannotations.SmokeTest
import org.mozilla.fenix.helpers.Constants.PackageName.GMAIL_APP
import org.mozilla.fenix.helpers.Constants.PackageName.PHONE_APP
import org.mozilla.fenix.helpers.FeatureSettingsHelper
import org.mozilla.fenix.helpers.HomeActivityIntentTestRule
import org.mozilla.fenix.helpers.TestHelper.assertNativeAppOpens
import org.mozilla.fenix.ui.robots.customTabScreen
import org.mozilla.fenix.ui.robots.navigationToolbar
import org.mozilla.fenix.ui.robots.pwaScreen

class PwaTest {
    private val featureSettingsHelper = FeatureSettingsHelper()
    /* Updated externalLinks.html to v2.0,
       changed the hypertext reference to mozilla-mobile.github.io/testapp/downloads for "External link"
     */
    private val externalLinksPWAPage = "https://mozilla-mobile.github.io/testapp/v2.0/externalLinks.html"

    // Updated TestApp versioning to v3.0, created a new HTMLforms.html
    private val htmlControlsPage = "https://andiaj.github.io/testapp/v3.0/HTMLforms.html"

    private val emailLink = "mailto://example@example.com"
    private val phoneLink = "tel://1234567890"
    private val shortcutTitle = "TEST_APP"
    private val hour = 10
    private val minute = 10
    private val colorHexValue = "#5b2067"

    @get:Rule
    val activityTestRule = HomeActivityIntentTestRule()

    @Before
    fun setUp() {
        featureSettingsHelper.disablePwaCFR(true)
    }

    @After
    fun tearDown() {
        featureSettingsHelper.resetAllFeatureFlags()
    }

    @SmokeTest
    @Test
    fun externalLinkPWATest() {
        val externalLinkURL = "https://mozilla-mobile.github.io/testapp/downloads"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(externalLinksPWAPage.toUri()) {
            waitForPageToLoad()
            verifyNotificationDotOnMainMenu()
        }.openThreeDotMenu {
        }.clickInstall {
            clickAddAutomaticallyButton()
        }.openHomeScreenShortcut(shortcutTitle) {
            clickLinkMatchingText("External link")
        }

        customTabScreen {
            verifyCustomTabToolbarTitle(externalLinkURL)
        }
    }

    @SmokeTest
    @Test
    fun emailLinkPWATest() {

        navigationToolbar {
        }.enterURLAndEnterToBrowser(externalLinksPWAPage.toUri()) {
            waitForPageToLoad()
            verifyNotificationDotOnMainMenu()
        }.openThreeDotMenu {
        }.clickInstall {
            clickAddAutomaticallyButton()
        }.openHomeScreenShortcut(shortcutTitle) {
            clickLinkMatchingText("Email link")
            assertNativeAppOpens(GMAIL_APP, emailLink)
        }
    }

    @SmokeTest
    @Test
    fun telephoneLinkPWATest() {

        navigationToolbar {
        }.enterURLAndEnterToBrowser(externalLinksPWAPage.toUri()) {
            waitForPageToLoad()
            verifyNotificationDotOnMainMenu()
        }.openThreeDotMenu {
        }.clickInstall {
            clickAddAutomaticallyButton()
        }.openHomeScreenShortcut(shortcutTitle) {
            clickLinkMatchingText("Telephone link")
            assertNativeAppOpens(PHONE_APP, phoneLink)
        }
    }

    @SmokeTest
    @Test
    fun cancelCalendarFormPWATest() {

        navigationToolbar {
        }.enterURLAndEnterToBrowser(htmlControlsPage.toUri()) {
            waitForPageToLoad()
            verifyNotificationDotOnMainMenu()
        }.openThreeDotMenu {
        }.clickInstall {
            clickAddAutomaticallyButton()
        }.openHomeScreenShortcut(shortcutTitle) {
        }
        pwaScreen {
            clickForm("Calendar Form", true)
            clickFormViewButton("CANCEL")
            clickSubmitDateButton()
            verifyNoDateIsSelected()
        }
    }

    @SmokeTest
    @Test
    fun setAndClearCalendarFormPWATest() {

        navigationToolbar {
        }.enterURLAndEnterToBrowser(htmlControlsPage.toUri()) {
            waitForPageToLoad()
            verifyNotificationDotOnMainMenu()
        }.openThreeDotMenu {
        }.clickInstall {
            clickAddAutomaticallyButton()
        }.openHomeScreenShortcut(shortcutTitle) {
        }
        pwaScreen {
            clickForm("Calendar Form", true)
            selectDate()
            clickFormViewButton("OK")
            clickSubmitDateButton()
            verifySelectedDate()
            clickForm("Calendar Form", true)
            clickFormViewButton("CLEAR")
            clickSubmitDateButton()
            verifyNoDateIsSelected()
        }
    }

    @SmokeTest
    @Test
    fun cancelClockFormPWATest() {

        navigationToolbar {
        }.enterURLAndEnterToBrowser(htmlControlsPage.toUri()) {
            waitForPageToLoad()
            verifyNotificationDotOnMainMenu()
        }.openThreeDotMenu {
        }.clickInstall {
            clickAddAutomaticallyButton()
        }.openHomeScreenShortcut(shortcutTitle) {
        }
        pwaScreen {
            clickForm("Clock Form", false)
            clickFormViewButton("CANCEL")
            clickSubmitTimeButton()
            verifyNoTimeIsSelected(hour, minute)
        }
    }

    @SmokeTest
    @Test
    fun setAndClearClockFormPWATest() {

        navigationToolbar {
        }.enterURLAndEnterToBrowser(htmlControlsPage.toUri()) {
            waitForPageToLoad()
            verifyNotificationDotOnMainMenu()
        }.openThreeDotMenu {
        }.clickInstall {
            clickAddAutomaticallyButton()
        }.openHomeScreenShortcut(shortcutTitle) {
        }
        pwaScreen {
            clickForm("Clock Form", clockForm = true)
            selectTime(hour, minute)
            clickFormViewButton("OK")
            clickSubmitTimeButton()
            verifySelectedTime(hour, minute)
            clickForm("Clock Form", clockForm = true)
            clickFormViewButton("CLEAR")
            clickSubmitTimeButton()
            verifyNoTimeIsSelected(hour, minute)
        }
    }

    @SmokeTest
    @Test
    fun cancelColorFormPWATest() {

        navigationToolbar {
        }.enterURLAndEnterToBrowser(htmlControlsPage.toUri()) {
            waitForPageToLoad()
            verifyNotificationDotOnMainMenu()
        }.openThreeDotMenu {
        }.clickInstall {
            clickAddAutomaticallyButton()
        }.openHomeScreenShortcut(shortcutTitle) {
        }
        pwaScreen {
            clickForm("Color Form")
            selectColor(colorHexValue)
            clickFormViewButton("CANCEL")
            clickSubmitColorButton()
            verifyColorIsNotSelected(colorHexValue)
        }
    }

    @SmokeTest
    @Test
    fun setColorFormPWATest() {

        navigationToolbar {
        }.enterURLAndEnterToBrowser(htmlControlsPage.toUri()) {
            waitForPageToLoad()
            verifyNotificationDotOnMainMenu()
        }.openThreeDotMenu {
        }.clickInstall {
            clickAddAutomaticallyButton()
        }.openHomeScreenShortcut(shortcutTitle) {
        }
        pwaScreen {
            clickForm("Color Form")
            selectColor(colorHexValue)
            clickFormViewButton("SET")
            clickSubmitColorButton()
            verifySelectedColor(colorHexValue)
        }
    }
}
