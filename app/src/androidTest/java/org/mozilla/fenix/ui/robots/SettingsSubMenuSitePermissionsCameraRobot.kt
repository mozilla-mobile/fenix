/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

/**
 * Implementation of Robot Pattern for the settings Site Permissions Camera sub menu.
 */

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.hamcrest.CoreMatchers
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.assertIsChecked
import org.mozilla.fenix.helpers.click

class SettingsSubMenuSitePermissionsCameraRobot {

    fun verifyNavigationToolBarHeader() = assertNavigationToolBarHeader()

    fun verifyassertAskToAllowRecommended() = assertAskToAllowRecommended()

    fun verifyassertBlocked() = assertBlocked()

    fun verifyCheckRadioButtonDefault() = assertCheckRadioButtonDefault()

    fun verifyBlockedByAndroid() = assertBlockedByAndroid()

    fun verifyToAllowIt() = assertToAllowIt()

    fun verifyGotoAndroidSettings() = assertGotoAndroidSettings()

    fun verifyTapPermissions() = assertTapPermissions()

    fun verifyToggleCameraToON() = assertToggleCameraToON()

    fun verifyGoToSettingsButton() = assertGoToSettingsButton()

    fun verifySitePermissionsCameraSubMenuItems() {
        verifyassertAskToAllowRecommended()
        verifyassertBlocked()
        verifyCheckRadioButtonDefault()
        verifyBlockedByAndroid()
        verifyToAllowIt()
        verifyGotoAndroidSettings()
        verifyTapPermissions()
        verifyToggleCameraToON()
        verifyGoToSettingsButton()
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
    onView(CoreMatchers.allOf(withContentDescription("Camera")))

private fun assertAskToAllowRecommended() = onView(withId(R.id.ask_to_allow_radio))
    .check((matches(withEffectiveVisibility(Visibility.VISIBLE))))

private fun assertBlocked() = onView(withId(R.id.block_radio))
    .check((matches(withEffectiveVisibility(Visibility.VISIBLE))))

private fun assertCheckRadioButtonDefault() {
    onView(withId(R.id.ask_to_allow_radio)).assertIsChecked(isChecked = true)
    onView(withId(R.id.block_radio)).assertIsChecked(isChecked = false)
}

private fun assertBlockedByAndroid() = onView(withText("Blocked by Android"))
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertToAllowIt() = onView(withText("To allow it:"))
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertGotoAndroidSettings() = onView(withText("1. Go to Android Settings"))
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertTapPermissions() = onView(withText("2. Tap Permissions"))
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertToggleCameraToON() = onView(withText("3. Toggle Camera to ON"))
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun assertGoToSettingsButton() = onView(withId(R.id.settings_button))
    .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

private fun goBackButton() =
    onView(CoreMatchers.allOf(withContentDescription("Navigate up")))
