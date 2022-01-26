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
    private val externalLinksPWAPage = "https://mozilla-mobile.github.io/testapp/externalLinks.html"
    private val clockAndCalendarPWAPage = "https://andiaj.github.io/testapp/clockAndCalendarForm.html"
    private val emailLink = "mailto://example@example.com"
    private val phoneLink = "tel://1234567890"
    private val shortcutTitle = "TEST_APP"
    private val hour = 10
    private val minute = 10

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
        val customTabTitle = "Google"

        navigationToolbar {
        }.enterURLAndEnterToBrowser(externalLinksPWAPage.toUri()) {
        }.openThreeDotMenu {
        }.clickInstall {
            clickAddAutomaticallyButton()
        }.openHomeScreenShortcut(shortcutTitle) {
            clickLinkMatchingText("External link")
            fillAndSubmitGoogleSearchQuery("Mozilla")
        }

        customTabScreen {
            verifyCustomTabToolbarTitle(customTabTitle)
        }
    }

    @SmokeTest
    @Test
    fun emailLinkPWATest() {

        navigationToolbar {
        }.enterURLAndEnterToBrowser(externalLinksPWAPage.toUri()) {
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
        }.enterURLAndEnterToBrowser(clockAndCalendarPWAPage.toUri()) {
        }.openThreeDotMenu {
        }.clickInstall {
            clickAddAutomaticallyButton()
        }.openHomeScreenShortcut(shortcutTitle) {
        }
        pwaScreen {
            clickForm("Calendar Form", true)
            clickClockAndCalendarViewButton("CANCEL")
            clickSubmitDateButton()
            verifyNoDateIsSelected()
        }
    }

    @SmokeTest
    @Test
    fun setAndClearCalendarFormPWATest() {

        navigationToolbar {
        }.enterURLAndEnterToBrowser(clockAndCalendarPWAPage.toUri()) {
        }.openThreeDotMenu {
        }.clickInstall {
            clickAddAutomaticallyButton()
        }.openHomeScreenShortcut(shortcutTitle) {
        }
        pwaScreen {
            clickForm("Calendar Form", true)
            selectDate()
            clickClockAndCalendarViewButton("OK")
            clickSubmitDateButton()
            verifySelectedDate()
            clickForm("Calendar Form", true)
            clickClockAndCalendarViewButton("CLEAR")
            clickSubmitDateButton()
            verifyNoDateIsSelected()
        }
    }

    @SmokeTest
    @Test
    fun cancelClockFormPWATest() {

        navigationToolbar {
        }.enterURLAndEnterToBrowser(clockAndCalendarPWAPage.toUri()) {
        }.openThreeDotMenu {
        }.clickInstall {
            clickAddAutomaticallyButton()
        }.openHomeScreenShortcut(shortcutTitle) {
        }
        pwaScreen {
            clickForm("Clock Form", false)
            clickClockAndCalendarViewButton("CANCEL")
            clickSubmitTimeButton()
            verifyNoTimeIsSelected(hour, minute)
        }
    }

    @SmokeTest
    @Test
    fun setAndClearClockFormPWATest() {

        navigationToolbar {
        }.enterURLAndEnterToBrowser(clockAndCalendarPWAPage.toUri()) {
        }.openThreeDotMenu {
        }.clickInstall {
            clickAddAutomaticallyButton()
        }.openHomeScreenShortcut(shortcutTitle) {
        }
        pwaScreen {
            clickForm("Clock Form", false)
            selectTime(hour, minute)
            clickClockAndCalendarViewButton("OK")
            clickSubmitTimeButton()
            verifySelectedTime(hour, minute)
            clickForm("Clock Form", false)
            clickClockAndCalendarViewButton("CLEAR")
            clickSubmitTimeButton()
            verifyNoTimeIsSelected(hour, minute)
        }
    }
}
