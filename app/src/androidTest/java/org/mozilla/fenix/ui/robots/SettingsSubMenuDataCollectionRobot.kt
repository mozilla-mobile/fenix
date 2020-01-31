/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withResourceName
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.hamcrest.CoreMatchers
import org.mozilla.fenix.helpers.click

/**
 * Implementation of Robot Pattern for the settings Delete Browsing Data sub menu.
 */
class SettingsSubMenuDataCollectionRobot {

    fun verifyUsageAndTechnicalDataMenuItem() = assertUsageAndTechnicalDataMenuItem()

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

private fun assertUsageAndTechnicalDataMenuItem() {
    onView(withText("Usage and technical data"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    onView(withText("Shares performance, usage, hardware and customization data about your browser with Mozilla to help us make Firefox Preview better"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    onView(withResourceName("switch_widget"))
        .check(matches(isChecked()))
}

fun settingsSubMenuDataCollection(interact: SettingsSubMenuDataCollectionRobot.() -> Unit): SettingsSubMenuDataCollectionRobot.Transition {
    SettingsSubMenuDataCollectionRobot().interact()
    return SettingsSubMenuDataCollectionRobot.Transition()
}

private fun goBackButton() =
    onView(CoreMatchers.allOf(ViewMatchers.withContentDescription("Navigate up")))
