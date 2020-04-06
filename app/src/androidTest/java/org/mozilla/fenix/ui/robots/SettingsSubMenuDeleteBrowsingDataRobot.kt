/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

/**
 * Implementation of Robot Pattern for the settings Delete Browsing Data sub menu.
 */

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.withChild
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.hamcrest.CoreMatchers.allOf
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.assertIsChecked
import org.mozilla.fenix.helpers.click
import org.mozilla.fenix.helpers.TestAssetHelper

class SettingsSubMenuDeleteBrowsingDataRobot {

    fun verifyNavigationToolBarHeader() = assertNavigationToolBarHeader()

    fun verifyDeleteBrowsingDataButton() = assertDeleteBrowsingDataButton()

    fun verifyClickDeleteBrowsingDataButton() = assertClickDeleteBrowsingDataButton()

    fun verifyMessageInDialogBox() = assertMessageInDialogBox()

    fun verifyDeleteButtonInDialogBox() = assertDeleteButtonInDialogBox()

    fun verifyCancelButtonInDialogBox() = assertCancelButtonInDialogBox()

    fun verifyAllTheCheckBoxesText() = assertAllTheCheckBoxesText()

    fun verifyAllTheCheckBoxesChecked() = assertAllTheCheckBoxesChecked()

    fun verifyContentsInDialogBox() {
        verifyMessageInDialogBox()
        verifyDeleteButtonInDialogBox()
        verifyCancelButtonInDialogBox()
    }

    fun clickCancelButtonInDialogBoxAndVerifyContentsInDialogBox() {
        mDevice.wait(
            Until.findObject(By.text("Delete browsing data")),
            TestAssetHelper.waitingTime
        )
        verifyClickDeleteBrowsingDataButton()
        verifyContentsInDialogBox()
        cancelButton().click()
    }

    fun verifyDeleteBrowsingDataSubMenuItems() {
        verifyDeleteBrowsingDataButton()
        clickCancelButtonInDialogBoxAndVerifyContentsInDialogBox()
        verifyAllTheCheckBoxesText()
        verifyAllTheCheckBoxesChecked()
    }

    class Transition {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())!!

        fun goBack(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            mDevice.waitForIdle()
            goBackButton().click()

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }
    }
}

private fun goBackButton() =
    onView(allOf(withContentDescription("Navigate up")))

private fun assertNavigationToolBarHeader() {
    onView(allOf(withId(R.id.navigationToolbar),
        withChild(withText("Delete browsing data"))))
        .check((matches(withEffectiveVisibility(Visibility.VISIBLE))))
}

private fun assertDeleteBrowsingDataButton() {
    onView(withId(R.id.delete_data))
        .check((matches(withEffectiveVisibility(Visibility.VISIBLE))))
}

private fun assertClickDeleteBrowsingDataButton() {
    onView(withId(R.id.delete_data))
        .check((matches(withEffectiveVisibility(Visibility.VISIBLE)))).click()
}

private fun cancelButton() =
    mDevice.findObject(UiSelector().textStartsWith("CANCEL"))

private fun assertMessageInDialogBox() =
    onView(withText("Firefox Preview will delete the selected browsing data."))
        .inRoot(isDialog())
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertDeleteButtonInDialogBox() =
    onView(withText("Delete"))
        .inRoot(isDialog())
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertCancelButtonInDialogBox() =
    onView(withText("Cancel"))
        .inRoot(isDialog())
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertAllTheCheckBoxesText() {

    onView(withText("Open Tabs"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    onView(withText("0 tabs"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    onView(withText("Browsing history and site data"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    onView(withText("0 addresses"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    onView(withText("Cookies"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    onView(withText("You’ll be logged out of most sites"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    onView(withText("Cached images and files"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    onView(withText("Frees up storage space"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    onView(withText("Site permissions"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertAllTheCheckBoxesChecked() {
    onView(allOf(withId(R.id.checkbox), hasSibling(withText("Open Tabs")))).assertIsChecked(true)

    onView(allOf(withId(R.id.checkbox), hasSibling(withText("Browsing history and site data")))).assertIsChecked(true)

    onView(allOf(withId(R.id.checkbox), hasSibling(withText("Cookies")))).assertIsChecked(true)

    onView(allOf(withId(R.id.checkbox), hasSibling(withText("Cached images and files")))).assertIsChecked(true)

    onView(allOf(withId(R.id.checkbox), hasSibling(withText("Site permissions")))).assertIsChecked(true)
}
