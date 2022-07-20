/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

import android.util.Log
import android.widget.TimePicker
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.uiautomator.UiSelector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.mozilla.fenix.helpers.Constants.RETRY_COUNT
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.TestHelper.mDevice
import org.mozilla.fenix.helpers.TestHelper.packageName
import java.time.LocalDate

class PwaRobot {

    fun clickForm(formType: String, calendarForm: Boolean = false, clockForm: Boolean = false) {
        mDevice.findObject(UiSelector().resourceId("$packageName:id/engineView"))
            .waitForExists(waitingTime)
        mDevice.findObject(UiSelector().textContains(formType)).waitForExists(waitingTime)

        if (calendarForm) {
            calendarBox.click()
            mDevice.waitForIdle(waitingTime)
        } else if (clockForm) {
            clockBox.click()
            mDevice.waitForIdle(waitingTime)
        } else {
            colorBox.click()
            mDevice.waitForIdle(waitingTime)
        }
    }

    fun clickFormViewButton(button: String) {
        val clockAndCalendarButton = mDevice.findObject(UiSelector().textContains(button))
        clockAndCalendarButton.click()
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

    fun selectTime(hour: Int, minute: Int) =
        onView(isAssignableFrom(TimePicker::class.java)).inRoot(RootMatchers.isDialog()).perform(PickerActions.setTime(hour, minute))

    fun selectColor(hexValue: String) {
        mDevice.findObject(
            UiSelector()
                .textContains("Choose a color")
                .resourceId("$packageName:id/alertTitle")
        ).waitForExists(waitingTime)

        val colorSelection =
            mDevice.findObject(
                UiSelector()
                    .resourceId("$packageName:id/color_item")
                    .descriptionContains(hexValue)
            )
        colorSelection.click()
    }

    fun clickSubmitDateButton() {
        submitDateButton.waitForExists(waitingTime)
        submitDateButton.click()
    }

    fun clickSubmitTimeButton() {
        submitTimeButton.waitForExists(waitingTime)
        submitTimeButton.click()
    }

    fun clickSubmitColorButton() {
        submitColorButton.waitForExists(waitingTime)
        submitColorButton.click()
    }

    fun verifySelectedDate() {
        for (i in 1..RETRY_COUNT) {
            try {
                assertTrue(
                    mDevice.findObject(
                        UiSelector()
                            .text("Selected date is: $currentDate")
                    ).waitForExists(waitingTime)
                )
            } catch (e: AssertionError) {
                Log.e("TestLog", "Selected time isn't displayed ${e.localizedMessage}")

                clickForm("Calendar Form", true)
                selectDate()
                clickFormViewButton("OK")
                clickSubmitDateButton()
            }
        }

        assertTrue(
            mDevice.findObject(
                UiSelector()
                    .text("Selected date is: $currentDate")
            ).waitForExists(waitingTime)
        )
    }

    fun verifyNoDateIsSelected() {
        assertFalse(
            mDevice.findObject(
                UiSelector()
                    .text("Selected date is: $currentDate")
            ).waitForExists(waitingTime)
        )
    }

    fun verifySelectedTime(hour: Int, minute: Int) {
        for (i in 1..RETRY_COUNT) {
            try {
                assertTrue(
                    mDevice.findObject(
                        UiSelector()
                            .text("Selected time is: $hour:$minute")
                    ).waitForExists(waitingTime)
                )

                break
            } catch (e: AssertionError) {
                Log.e("TestLog", "Selected time isn't displayed ${e.localizedMessage}")

                clickForm("Clock Form", clockForm = true)
                clickFormViewButton("CLEAR")
                clickForm("Clock Form", clockForm = true)
                selectTime(hour, minute)
                clickFormViewButton("OK")
                clickSubmitTimeButton()
            }
        }

        assertTrue(
            mDevice.findObject(
                UiSelector()
                    .text("Selected time is: $hour:$minute")
            ).waitForExists(waitingTime)
        )
    }

    fun verifySelectedColor(hexValue: String) {
        for (i in 1..RETRY_COUNT) {
            try {
                assertTrue(
                    mDevice.findObject(
                        UiSelector()
                            .text("Selected color is: $hexValue")
                    ).waitForExists(waitingTime)
                )

                break
            } catch (e: AssertionError) {
                Log.e("TestLog", "Selected color isn't displayed ${e.localizedMessage}")

                clickForm("Color Form")
                selectColor(hexValue)
                clickFormViewButton("SET")
                clickSubmitColorButton()
            }
        }

        assertTrue(
            mDevice.findObject(
                UiSelector()
                    .text("Selected color is: $hexValue")
            ).waitForExists(waitingTime)
        )
    }

    fun verifyNoTimeIsSelected(hour: Int, minute: Int) {
        assertFalse(
            mDevice.findObject(
                UiSelector()
                    .text("Selected date is: $hour:$minute")
            ).waitForExists(waitingTime)
        )
    }

    fun verifyColorIsNotSelected(hexValue: String) {
        assertFalse(
            mDevice.findObject(
                UiSelector()
                    .text("Selected date is: $hexValue")
            ).waitForExists(waitingTime)
        )
    }

    class Transition
}

fun pwaScreen(interact: PwaRobot.() -> Unit): PwaRobot.Transition {
    PwaRobot().interact()
    return PwaRobot.Transition()
}

val calendarBox =
    mDevice.findObject(
        UiSelector()
            .index(0)
            .resourceId("calendar")
            .className("android.widget.Spinner")
            .packageName("$packageName")
    )

val clockBox =
    mDevice.findObject(
        UiSelector()
            .index(0)
            .resourceId("clock")
            .className("android.view.View")
            .packageName("$packageName")
    )

val colorBox =
    mDevice.findObject(
        UiSelector()
            .index(0)
            .resourceId("colorPicker")
            .className("android.widget.Button")
            .packageName("$packageName")
    )

val currentDate = LocalDate.now()
val currentDay = currentDate.dayOfMonth
val currentMonth = currentDate.month
val currentYear = currentDate.year

val submitDateButton =
    mDevice.findObject(
        UiSelector()
            .textContains("Submit date")
            .resourceId("submitDate")
    )

val submitTimeButton =
    mDevice.findObject(
        UiSelector()
            .textContains("Submit time")
            .resourceId("submitTime")
    )

val submitColorButton =
    mDevice.findObject(
        UiSelector()
            .textContains("Submit color")
            .resourceId("submitColor")
    )
