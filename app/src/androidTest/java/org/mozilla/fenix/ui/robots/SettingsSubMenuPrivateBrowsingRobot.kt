/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.robots

import android.os.Build
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.withChild
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.By.text
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.hamcrest.CoreMatchers
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.click
import org.mozilla.fenix.helpers.isEnabled

/**
 * Implementation of Robot Pattern for the settings PrivateBrowsing sub menu.
 */

class SettingsSubMenuPrivateBrowsingRobot {

    fun verifyNavigationToolBarHeader() = assertNavigationToolBarHeader()

    fun verifyOpenLinksInPrivateTab() = assertOpenLinksInPrivateTab()

    fun verifyAddPrivateBrowsingShortcutButton() = assertAddPrivateBrowsingShortcutButton()

    fun verifyOpenLinksInPrivateTabEnabled() = assertOpenLinksInPrivateTabEnabled()

    fun verifyOpenLinksInPrivateTabOff() = assertOpenLinksInPrivateTabOff()

    fun verifyPrivateBrowsingShortcutIcon() = assertPrivateBrowsingShortcutIcon()

    fun clickOpenLinksInPrivateTabSwitch() = openLinksInPrivateTabSwitch().click()

    fun addPrivateShortcutToHomescreen() {
        mDevice.wait(
            Until.findObject(text("Add private browsing shortcut")),
            waitingTime
        )
        addPrivateBrowsingShortcutButton().click()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mDevice.wait(Until.findObject(By.textContains("add automatically")), waitingTime)
            addAutomaticallyButton().click()
        }
    }

    class Transition {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        fun goBack(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            goBackButton().perform(ViewActions.click())

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }

        fun openPrivateBrowsingShortcut(interact: SearchRobot.() -> Unit): SearchRobot.Transition {
            privateBrowsingShortcutIcon().click()

            SearchRobot().interact()
            return SearchRobot.Transition()
        }
    }
}

private fun assertNavigationToolBarHeader() {
    onView(
        CoreMatchers.allOf(
            withId(R.id.navigationToolbar),
            withChild(withText(R.string.preferences_private_browsing_options))
        )
    )
        .check((matches(withEffectiveVisibility(Visibility.VISIBLE))))
}

private fun openLinksInPrivateTabSwitch() =
    onView(withText("Open links in a private tab"))

private fun addPrivateBrowsingShortcutButton() = onView(withText("Add private browsing shortcut"))

private fun goBackButton() = onView(withContentDescription("Navigate up"))

private fun addAutomaticallyButton() =
    mDevice.findObject(UiSelector().textStartsWith("add automatically"))

private fun privateBrowsingShortcutIcon() = mDevice.findObject(text("Private Firefox Preview"))

private fun assertAddPrivateBrowsingShortcutButton() {
    mDevice.wait(
        Until.findObject(text("Add private browsing shortcut")),
        waitingTime
    )
    addPrivateBrowsingShortcutButton()
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertOpenLinksInPrivateTab() {
    openLinksInPrivateTabSwitch()
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertOpenLinksInPrivateTabEnabled() =
    openLinksInPrivateTabSwitch().check(matches(isEnabled(true)))

private fun assertOpenLinksInPrivateTabOff() {
    assertFalse(mDevice.findObject(UiSelector().resourceId("android:id/switch_widget")).isChecked())
    openLinksInPrivateTabSwitch()
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertPrivateBrowsingShortcutIcon() {
    mDevice.wait(
        Until.findObject(text("Private Firefox Preview")),
        waitingTime
    )
    assertTrue(mDevice.hasObject(text("Private Firefox Preview")))
}
