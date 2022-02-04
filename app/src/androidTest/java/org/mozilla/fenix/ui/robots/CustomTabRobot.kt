/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.fenix.ui.robots

import android.widget.TimePicker
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiSelector
import junit.framework.TestCase.assertTrue
import org.junit.Assert
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.Constants.LONG_CLICK_DURATION
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.TestHelper.appName
import org.mozilla.fenix.helpers.TestHelper.packageName
import org.mozilla.fenix.helpers.TestHelper.waitForObjects
import java.time.LocalDate

/**
 *  Implementation of the robot pattern for Custom tabs
 */
class CustomTabRobot {

    fun verifyDesktopSiteButtonExists() {
        desktopSiteButton().check(matches(isDisplayed()))
    }

    fun verifyFindInPageButtonExists() {
        findInPageButton().check(matches(isDisplayed()))
    }

    fun verifyPoweredByTextIsDisplayed() {
        assertTrue(
            mDevice.findObject(UiSelector().textContains("POWERED BY $appName"))
                .waitForExists(waitingTime)
        )
    }

    fun verifyOpenInBrowserButtonExists() {
        openInBrowserButton().check(matches(isDisplayed()))
    }

    fun verifyBackButtonExists() = assertTrue(backButton().waitForExists(waitingTime))

    fun verifyForwardButtonExists() = assertTrue(forwardButton().waitForExists(waitingTime))

    fun verifyRefreshButtonExists() = assertTrue(refreshButton().waitForExists(waitingTime))

    fun verifyCustomMenuItem(label: String) {
        assertTrue(mDevice.findObject(UiSelector().text(label)).waitForExists(waitingTime))
    }

    fun verifyCustomTabCloseButton() {
        closeButton().check(matches(isDisplayed()))
    }

    fun verifyCustomTabToolbarTitle(title: String) {
        mDevice.waitForObjects(
            mDevice.findObject(
                UiSelector()
                    .resourceId("$packageName:id/mozac_browser_toolbar_title_view")
                    .textContains(title)
            )
        )

        assertTrue(
            mDevice.findObject(
                UiSelector()
                    .resourceId("$packageName:id/mozac_browser_toolbar_title_view")
                    .textContains(title)
            ).waitForExists(waitingTime)
        )
    }

    fun longCLickAndCopyToolbarUrl() {
        mDevice.waitForObjects(mDevice.findObject(UiSelector().resourceId("$packageName:id/toolbar")))
        customTabToolbar().click(LONG_CLICK_DURATION)
        mDevice.findObject(UiSelector().textContains("Copy")).waitForExists(waitingTime)
        val copyText = mDevice.findObject(By.textContains("Copy"))
        copyText.click()
    }

    fun waitForPageToLoad() = progressBar.waitUntilGone(waitingTime)

    fun clickLinkMatchingText(expectedText: String) {
        mDevice.findObject(UiSelector().resourceId("$packageName:id/engineView"))
            .waitForExists(waitingTime)
        mDevice.findObject(UiSelector().textContains(expectedText)).waitForExists(waitingTime)

        val element = mDevice.findObject(UiSelector().textContains(expectedText))
        element.click()
    }

    fun fillAndSubmitLoginCredentials(userName: String, password: String) {
        userNameTextBox.setText(userName)
        passwordTextBox.setText(password)

        submitLoginButton.click()

        mDevice.waitForObjects(mDevice.findObject(UiSelector().resourceId("$packageName:id/save_confirm")))
    }

    fun verifySaveLoginPromptIsDisplayed() {
        Assert.assertTrue(
            mDevice.findObject(
                UiSelector()
                    .resourceId("$packageName:id/feature_prompt_login_fragment")
            ).waitForExists(waitingTime)
        )
    }

    fun saveLoginFromPrompt(optionToSaveLogin: String) {
        mDevice.waitForObjects(
            mDevice.findObject(
                UiSelector().resourceId("$packageName:id/feature_prompt_login_fragment")
            )
        )
        mDevice.findObject(By.text(optionToSaveLogin)).click()
    }

    fun clickForm(formType: String, calendarForm: Boolean = false) {
        mDevice.findObject(UiSelector().resourceId("$packageName:id/engineView"))
            .waitForExists(waitingTime)
        mDevice.findObject(UiSelector().textContains(formType)).waitForExists(waitingTime)

        if (calendarForm) {
            calendarBox.click()
            mDevice.waitForIdle(waitingTime)
        } else {
            clockBox.click()
            mDevice.waitForIdle(waitingTime)
        }
    }

    fun selectDate() {
        mDevice.findObject(UiSelector().resourceId("android:id/month_view")).waitForExists(waitingTime)

        val monthViewerCurrentDay =
            mDevice.findObject(
                UiSelector()
                    .textContains("$currentDay")
                    .descriptionContains("$currentDay $currentMonth $currentYear")
            )

        monthViewerCurrentDay.click()
    }

    fun clickFormViewButton(button: String) {
        val clockAndCalendarButton = mDevice.findObject(UiSelector().textContains(button))
        clockAndCalendarButton.click()
    }

    fun clickSubmitDateButton() {
        submitDateButton.waitForExists(waitingTime)
        submitDateButton.click()
    }

    fun verifySelectedDate() {
        mDevice.findObject(
            UiSelector()
                .textContains("Submit date")
                .resourceId("submitDate")
        ).waitForExists(waitingTime)

        Assert.assertTrue(
            mDevice.findObject(
                UiSelector()
                    .text("Selected date is: $currentDate")
            ).waitForExists(waitingTime)
        )
    }

    fun selectTime(hour: Int, minute: Int) =
        onView(ViewMatchers.isAssignableFrom(TimePicker::class.java)).perform(PickerActions.setTime(hour, minute))

    fun clickSubmitTimeButton() {
        submitTimeButton.waitForExists(waitingTime)
        submitTimeButton.click()
    }

    fun verifySelectedTime(hour: Int, minute: Int) {
        mDevice.findObject(
            UiSelector()
                .textContains("Submit date")
                .resourceId("submitDate")
        ).waitForExists(waitingTime)

        Assert.assertTrue(
            mDevice.findObject(
                UiSelector()
                    .text("Selected time is: $hour:$minute")
            ).waitForExists(waitingTime)
        )
    }

    fun clickDropDownForm() {
        mDevice.findObject(UiSelector().resourceId("$packageName:id/engineView"))
            .waitForExists(waitingTime)
        mDevice.findObject(UiSelector().textContains("Drop-down Form")).waitForExists(waitingTime)
        dropDownForm.click()
    }

    fun selectDropDownOption(optionName: String) {
        mDevice.waitForObjects(mDevice.findObject(UiSelector().resourceId("$packageName:id/customPanel")))
        val dropDownOption = mDevice.findObject(UiSelector().textContains(optionName))
        dropDownOption.click()
    }

    fun clickSubmitDropDownButton() {
        submitDropDownButton.waitForExists(waitingTime)
        submitDropDownButton.click()
    }

    fun verifySelectedDropDownOption(optionName: String) {
        mDevice.findObject(
            UiSelector()
                .textContains("Submit drop down option")
                .resourceId("submitOption")
        ).waitForExists(waitingTime)

        Assert.assertTrue(
            mDevice.findObject(
                UiSelector()
                    .text("Selected option is: $optionName")
            ).waitForExists(waitingTime)
        )
    }

    class Transition {
        fun openMainMenu(interact: CustomTabRobot.() -> Unit): Transition {
            mainMenuButton().waitForExists(waitingTime)
            mainMenuButton().click()

            CustomTabRobot().interact()
            return Transition()
        }

        fun clickOpenInBrowserButton(interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            openInBrowserButton().perform(click())

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun clickStartCameraButton(interact: SitePermissionsRobot.() -> Unit): SitePermissionsRobot.Transition {
            cameraButton.waitForExists(waitingTime)
            cameraButton.click()

            SitePermissionsRobot().interact()
            return SitePermissionsRobot.Transition()
        }
    }
}

fun customTabScreen(interact: CustomTabRobot.() -> Unit): CustomTabRobot.Transition {
    CustomTabRobot().interact()
    return CustomTabRobot.Transition()
}

private fun mainMenuButton() = mDevice.findObject(UiSelector().description("Menu"))

private fun desktopSiteButton() = onView(withId(R.id.switch_widget))

private fun findInPageButton() = onView(withText("Find in page"))

private fun openInBrowserButton() = onView(withText("Open in $appName"))

private fun refreshButton() = mDevice.findObject(UiSelector().description("Refresh"))

private fun forwardButton() = mDevice.findObject(UiSelector().description("Forward"))

private fun backButton() = mDevice.findObject(UiSelector().description("Back"))

private fun closeButton() = onView(withContentDescription("Return to previous app"))

private fun customTabToolbar() = mDevice.findObject(By.res("$packageName:id/toolbar"))

private val progressBar =
    mDevice.findObject(
        UiSelector().resourceId("$packageName:id/mozac_browser_toolbar_progress")
    )

private val submitLoginButton =
    mDevice.findObject(
        UiSelector()
            .index(2)
            .resourceId("submit")
            .textContains("Submit Query")
            .className("android.widget.Button")
            .packageName("$packageName")
    )

private val cameraButton =
    mDevice.findObject(
        UiSelector()
            .index(1)
            .resourceId("video")
            .textContains("Open camera")
            .className("android.widget.Button")
            .packageName("$packageName")
    )

private val calendarBox =
    mDevice.findObject(
        UiSelector()
            .index(0)
            .resourceId("calendar")
            .className("android.widget.Spinner")
            .packageName("$packageName")
    )

private val clockBox =
    mDevice.findObject(
        UiSelector()
            .index(0)
            .resourceId("clock")
            .className("android.view.View")
            .packageName("$packageName")
    )

private val dropDownForm =
    mDevice.findObject(
        UiSelector()
            .index(13)
            .resourceId("dropDown")
            .className("android.widget.Spinner")
            .packageName("$packageName")
    )

private val submitTimeButton =
    mDevice.findObject(
        UiSelector()
            .textContains("Submit time")
            .resourceId("submitTime")
    )

private val submitDateButton =
    mDevice.findObject(
        UiSelector()
            .textContains("Submit date")
            .resourceId("submitDate")
    )

private val submitDropDownButton =
    mDevice.findObject(
        UiSelector()
            .textContains("Submit drop down option")
            .resourceId("submitOption")
    )

val currentDate = LocalDate.now()
val currentDay = currentDate.dayOfMonth
val currentMonth = currentDate.month
val currentYear = currentDate.year
