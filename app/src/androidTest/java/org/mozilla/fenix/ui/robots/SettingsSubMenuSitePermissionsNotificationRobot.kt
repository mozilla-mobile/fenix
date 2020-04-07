/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

/**
 * Implementation of Robot Pattern for the settings Site Permissions Notification sub menu.
 */

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.hamcrest.CoreMatchers
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.assertIsChecked
import org.mozilla.fenix.helpers.click

class SettingsSubMenuSitePermissionsNotificationRobot {

    fun verifyNavigationToolBarHeader() = assertNavigationToolBarHeader()

    fun verifyassertAskToAllowRecommended() = assertAskToAllowRecommended()

    fun verifyassertBlocked() = assertBlocked()

    fun verifyCheckRadioButtonDefault() = assertCheckRadioButtonDefault()

    fun verifySitePermissionsNotificationSubMenuItems() {
        verifyassertAskToAllowRecommended()
        verifyassertBlocked()
        verifyCheckRadioButtonDefault()
    }

    class Transition {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())!!

        fun goBack(interact: SettingsSubMenuSitePermissionsRobot.() -> Unit): SettingsSubMenuSitePermissionsRobot.Transition {
            mDevice.waitForIdle()
            goBackButton().click()

            SettingsSubMenuSitePermissionsRobot().interact()
            return SettingsSubMenuSitePermissionsRobot.Transition()
        }
    }
}

private fun assertNavigationToolBarHeader() =
    onView(CoreMatchers.allOf(withContentDescription("Notification")))

private fun assertAskToAllowRecommended() = onView(withId(R.id.ask_to_allow_radio))
    .check((ViewAssertions.matches(withEffectiveVisibility(Visibility.VISIBLE))))

private fun assertBlocked() = onView(withId(R.id.block_radio))
    .check((ViewAssertions.matches(withEffectiveVisibility(Visibility.VISIBLE))))

private fun assertCheckRadioButtonDefault() {
    onView(withId(R.id.ask_to_allow_radio)).assertIsChecked(isChecked = true)
    onView(withId(R.id.block_radio)).assertIsChecked(isChecked = false)
}

private fun goBackButton() =
    onView(CoreMatchers.allOf(withContentDescription("Navigate up")))
