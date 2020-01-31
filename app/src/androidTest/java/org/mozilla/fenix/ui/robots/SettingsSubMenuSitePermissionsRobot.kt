/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.hamcrest.CoreMatchers
import org.mozilla.fenix.helpers.click

/**
 * Implementation of Robot Pattern for the settings Site Permissions sub menu.
 */
class SettingsSubMenuSitePermissionsRobot {

    fun verifyMenuItems() = assertMenuItems()
    fun updatePermissionToBlocked(permission: String) = changePermissionToBlocked(permission)
    fun updateAutoplayPermissionToBlocked() = changeAutoplayPermissionToBlocked()
    fun updateAutoplayPermissionToAllowed() = changeAutoplayPermissionToAllowed()

    class Transition {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())!!

        fun goBack(interact: SettingsRobot.() -> Unit): SettingsRobot.Transition {
            mDevice.waitForIdle()
            goBackButton().click()

            SettingsRobot().interact()
            return SettingsRobot.Transition()
        }

        fun openExceptions(interact: SettingsSubMenuSitePermissionsExceptionsRobot.() -> Unit): SettingsSubMenuSitePermissionsExceptionsRobot.Transition {
            exceptionsButton().click()

            SettingsSubMenuSitePermissionsExceptionsRobot().interact()
            return SettingsSubMenuSitePermissionsExceptionsRobot.Transition()
        }
    }
}

private fun changePermissionToBlocked(permission: String) {
    onView(withText(permission)).click()
    onView(withText("Blocked")).click()
    Espresso.pressBack()
}

private fun changeAutoplayPermissionToBlocked() {
    onView(withText("Autoplay")).click()
    onView(withText("Video and audio blocked")).click()
    Espresso.pressBack()
}

private fun changeAutoplayPermissionToAllowed() {
    onView(withText("Autoplay")).click()
    onView(withText("Video and audio allowed")).click()
    Espresso.pressBack()
}

private fun assertMenuItems() {
    autoplayButton()
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    cameraButton()
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    locationButton()
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    microphoneButton()
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    notificationButton()
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    exceptionsButton()
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

fun settingsSubMenuSitePermissions(interact: SettingsSubMenuSitePermissionsRobot.() -> Unit): SettingsSubMenuSitePermissionsRobot.Transition {
    SettingsSubMenuSitePermissionsRobot().interact()
    return SettingsSubMenuSitePermissionsRobot.Transition()
}

private fun goBackButton() = onView(CoreMatchers.allOf(withContentDescription("Navigate up")))

private fun autoplayButton() = onView(withText("Autoplay"))

private fun cameraButton() = onView(withText("Camera"))

private fun locationButton() = onView(withText("Location"))

private fun microphoneButton() = onView(withText("Microphone"))

private fun notificationButton() = onView(withText("Notification"))

private fun exceptionsButton() = onView(withText("Exceptions"))
