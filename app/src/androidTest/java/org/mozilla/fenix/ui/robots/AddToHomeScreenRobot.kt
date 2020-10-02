/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.robots

import android.os.Build
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.By
import androidx.test.uiautomator.By.text
import androidx.test.uiautomator.By.textContains
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.anyOf
import org.junit.Assert
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.click
import org.mozilla.fenix.helpers.ext.waitNotNull

/**
 * Implementation of Robot Pattern for the Add to homescreen feature.
 */
class AddToHomeScreenRobot {

    fun verifyShortcutIcon() = assertShortcutIcon()

    fun addShortcutName(title: String) {
        mDevice.waitNotNull(Until.findObject(By.text("Add to Home screen")), waitingTime)
        shortcutNameField()
            .perform(clearText())
            .perform(typeText(title))
    }

    fun verifyShortcutNameField(expectedText: String) = assertShortcutNameField(expectedText)

    fun clickAddShortcutButton() = addButton().click()

    fun clickAddAutomaticallyButton() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mDevice.wait(Until.findObject(textContains("add automatically")), waitingTime)
            addAutomaticallyButton().click()
        }
    }

    class Transition {
        fun openHomeScreenShortcut(title: String, interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            mDevice.wait(
                Until.findObject(By.text(title)),
                waitingTime
            )
            mDevice.findObject((UiSelector().text(title))).clickAndWaitForNewWindow(waitingTime)

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }

        fun searchAndOpenHomeScreenShortcut(title: String, interact: BrowserRobot.() -> Unit): BrowserRobot.Transition {
            mDevice.pressHome()

            fun homeScreenView() = UiScrollable(UiSelector().scrollable(true))
            homeScreenView().setAsHorizontalList()

            fun shortcut() =
                homeScreenView().getChildByText(UiSelector().textContains(title), title)
            shortcut().clickAndWaitForNewWindow()

            BrowserRobot().interact()
            return BrowserRobot.Transition()
        }
    }
}

fun addToHomeScreen(interact: AddToHomeScreenRobot.() -> Unit): AddToHomeScreenRobot.Transition {
    AddToHomeScreenRobot().interact()
    return AddToHomeScreenRobot.Transition()
}

private fun shortcutNameField() = onView(withId(R.id.shortcut_text))

private fun assertShortcutNameField(expectedText: String) {
    onView(allOf(withId(R.id.shortcut_text),
        withText(expectedText)))
        .check(matches(isCompletelyDisplayed()))
}

private fun addButton() = onView(anyOf(withText("ADD")))

private fun addAutomaticallyButton() =
    mDevice.findObject(UiSelector().textContains("add automatically"))

private fun assertShortcutIcon() {
    mDevice.wait(
        Until.findObject(text("Firefox Preview")),
        waitingTime
    )
    Assert.assertTrue(mDevice.hasObject(By.text("Firefox Preview")))
}
