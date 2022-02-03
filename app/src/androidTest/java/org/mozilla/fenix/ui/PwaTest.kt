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
import org.mozilla.fenix.helpers.TestHelper.openAppFromExternalLink
import org.mozilla.fenix.ui.robots.addToHomeScreen
import org.mozilla.fenix.ui.robots.browserScreen
import org.mozilla.fenix.ui.robots.customTabScreen
import org.mozilla.fenix.ui.robots.mDevice
import org.mozilla.fenix.ui.robots.navigationToolbar
import org.mozilla.fenix.ui.robots.pwaScreen

class PwaTest {
    private val featureSettingsHelper = FeatureSettingsHelper()
    private val htmlControlsPWAPage = "https://andiaj.github.io/testapp/htmlControlsForm.html"
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
        val searchTerm = "Mozilla"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(htmlControlsPWAPage.toUri()) {
        }.openThreeDotMenu {
        }.clickInstall {
            clickAddAutomaticallyButton()
        }.openHomeScreenShortcut(shortcutTitle) {
            clickLinkMatchingText("External link")
            fillAndSubmitDuckDuckGoSearchQuery(searchTerm)
        }

        customTabScreen {
            verifyCustomTabToolbarTitle(searchTerm)
        }
    }

    @SmokeTest
    @Test
    fun emailLinkPWATest() {

        navigationToolbar {
        }.enterURLAndEnterToBrowser(htmlControlsPWAPage.toUri()) {
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
        }.enterURLAndEnterToBrowser(htmlControlsPWAPage.toUri()) {
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
    fun saveLoginsInPWATest() {

        navigationToolbar {
        }.enterURLAndEnterToBrowser(htmlControlsPWAPage.toUri()) {
            verifyNotificationDotOnMainMenu()
        }.openThreeDotMenu {
        }.clickInstall {
            clickAddAutomaticallyButton()
        }.openHomeScreenShortcut(shortcutTitle) {
            mDevice.waitForIdle()
            fillAndSubmitLoginCredentials("mozilla", "firefox")
            verifySaveLoginPromptIsDisplayed()
            saveLoginFromPrompt("Save")
            openAppFromExternalLink(htmlControlsPWAPage)

            browserScreen {
            }.openThreeDotMenu {
            }.openSettings {
            }.openLoginsAndPasswordSubMenu {
            }.openSavedLogins {
                verifySecurityPromptForLogins()
                tapSetupLater()
                verifySavedLoginFromPrompt("mozilla")
            }

            addToHomeScreen {
            }.searchAndOpenHomeScreenShortcut(shortcutTitle) {
                verifyPrefilledLoginCredentials("mozilla", shortcutTitle)
            }
        }
    }

    @SmokeTest
    @Test
    fun cancelCalendarFormPWATest() {

        navigationToolbar {
        }.enterURLAndEnterToBrowser(htmlControlsPWAPage.toUri()) {
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
        }.enterURLAndEnterToBrowser(htmlControlsPWAPage.toUri()) {
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
        }.enterURLAndEnterToBrowser(htmlControlsPWAPage.toUri()) {
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
        }.enterURLAndEnterToBrowser(htmlControlsPWAPage.toUri()) {
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
        }.enterURLAndEnterToBrowser(htmlControlsPWAPage.toUri()) {
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
        }.enterURLAndEnterToBrowser(htmlControlsPWAPage.toUri()) {
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
