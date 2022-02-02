package org.mozilla.fenix.ui.robots

import android.widget.TimePicker
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.uiautomator.UiSelector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.TestHelper
import org.mozilla.fenix.helpers.TestHelper.packageName
import java.time.LocalDate

class PwaRobot {

    fun clickForm(formType: String, calendarForm: Boolean = false) {
        mDevice.findObject(UiSelector().resourceId("$packageName:id/engineView"))
            .waitForExists(waitingTime)
        mDevice.findObject(UiSelector().textContains(formType)).waitForExists(waitingTime)

        if (calendarForm) {
            calendarBox.click()
        } else {
            clockBox.click()
        }
    }

    fun clickClockAndCalendarViewButton(button: String) {
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
        onView(isAssignableFrom(TimePicker::class.java)).perform(PickerActions.setTime(hour, minute))

    fun clickSubmitDateButton() {
        submitDateButton.waitForExists(waitingTime)
        submitDateButton.click()
    }

    fun clickSubmitTimeButton() {
        submitTimeButton.waitForExists(waitingTime)
        submitTimeButton.click()
    }

    fun verifySelectedDate() {
        mDevice.findObject(
            UiSelector()
                .textContains("Submit date")
                .resourceId("submitDate")
        ).waitForExists(waitingTime)

        assertTrue(
            mDevice.findObject(
                UiSelector()
                    .text("Selected date is: $currentDate")
            ).waitForExists(waitingTime)
        )
    }

    fun verifyNoDateIsSelected() {
        mDevice.findObject(
            UiSelector()
                .textContains("Submit date")
                .resourceId("submitDate")
        ).waitForExists(waitingTime)

        assertFalse(
            mDevice.findObject(
                UiSelector()
                    .text("Selected date is: $currentDate")
            ).waitForExists(waitingTime)
        )
    }

    fun verifySelectedTime(hour: Int, minute: Int) {
        mDevice.findObject(
            UiSelector()
                .textContains("Submit date")
                .resourceId("submitDate")
        ).waitForExists(waitingTime)

        assertTrue(
            mDevice.findObject(
                UiSelector()
                    .text("Selected time is: $hour:$minute")
            ).waitForExists(waitingTime)
        )
    }

    fun verifyNoTimeIsSelected(hour: Int, minute: Int) {
        mDevice.findObject(
            UiSelector()
                .textContains("Submit date")
                .resourceId("submitDate")
        ).waitForExists(waitingTime)

        assertFalse(
            mDevice.findObject(
                UiSelector()
                    .text("Selected date is: $hour:$minute")
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
            .packageName("${TestHelper.packageName}")
    )

val clockBox =
    mDevice.findObject(
        UiSelector()
            .index(0)
            .resourceId("clock")
            .className("android.view.View")
            .packageName("${TestHelper.packageName}")
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

val amClockButton =
    mDevice.findObject(
        UiSelector()
            .index(0)
            .textContains("AM")
            .className("android.widget.RadioButton")
    )
