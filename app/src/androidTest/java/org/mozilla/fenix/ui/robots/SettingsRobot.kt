/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.By.text
import androidx.test.uiautomator.Until
import org.hamcrest.CoreMatchers
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.click

/**
 * Implementation of Robot Pattern for the settings menu.
 */
class SettingsRobot {

    fun verifySettingsView() = assertSettingsView()
    class Transition {

        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        fun goBack(interact: HomeScreenRobot.() -> Unit): HomeScreenRobot.Transition {

            mDevice.waitForIdle()
            goBackButton().perform(ViewActions.click())

            HomeScreenRobot().interact()
            return HomeScreenRobot.Transition()
        }

        fun openAboutMenu(interact: AboutRobot.() -> Unit): AboutRobot.Transition {
            swipeToBottom()
            mDevice.wait(
                Until.findObject(text("R.string.preferences_about")),
                TestAssetHelper.waitingTime
            )
            aboutButton().click()

            AboutRobot().interact()
            return AboutRobot.Transition()
        }
    }
}

private fun assertSettingsView() {
    // verify that we are in the correct library view
    assertBasicsHeading()
    assertPrivacyHeading()
}

private fun assertBasicsHeading() = onView(ViewMatchers.withText("Basics"))
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
private fun assertPrivacyHeading() = onView(ViewMatchers.withText("Privacy"))
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun goBackButton() = onView(CoreMatchers.allOf(withContentDescription("Navigate up")))
private val appName = InstrumentationRegistry.getInstrumentation().targetContext.resources.getString(R.string.app_name)
private val aboutAppHeading = "About $appName"
private fun aboutButton() = onView(ViewMatchers.withText(aboutAppHeading))
private fun swipeToBottom() = onView(ViewMatchers.withId(R.id.recycler_view)).perform(ViewActions.swipeUp())
