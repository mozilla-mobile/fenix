/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.hamcrest.CoreMatchers.allOf
import org.junit.Assert.assertTrue
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.TestAssetHelper.waitingTime
import org.mozilla.fenix.helpers.TestHelper.packageName
import org.mozilla.fenix.helpers.click

/**
 * Implementation of Robot Pattern for the settings Site Permissions Notification sub menu.
 */
class SettingsSubMenuSitePermissionsExceptionsRobot {

    fun verifyNavigationToolBarHeader() = assertNavigationToolBarHeader()

    fun verifyExceptionDefault() = assertExceptionDefault()!!

    fun verifySitePermissionsExceptionSubMenuItems() {
        verifyExceptionDefault()
    }

    fun verifyWebSiteInExceptionList() = assertWebSiteInExceptionList()

    fun openWebSiteInExceptionList() =
        mDevice.findObject(UiSelector().textContains("localhost:")).click()

    fun verifyCameraPermissionStatus(state: String) = assertCameraPermissionStatus(state)

    fun openCameraPermission() =
        mDevice.findObject(UiSelector().textContains("Camera")).click()

    fun verifyCameraPermissionView() = assertCameraPermissionView()

    fun clickClearPermissionButton() = clearPermissionButton().click()

    fun verifyConfirmationDialog() = assertConfirmationDialog()

    fun clickCancelDialogButton() =
        mDevice.findObject(UiSelector().textContains("CANCEL")).click()

    fun clickOkDialogButton() =
        mDevice.findObject(UiSelector().textContains("OK")).click()

    class Transition {
        val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())!!

        fun goBack(interact: SettingsSubMenuSitePermissionsRobot.() -> Unit): SettingsSubMenuSitePermissionsRobot.Transition {
            goBackButton().click()

            SettingsSubMenuSitePermissionsRobot().interact()
            return SettingsSubMenuSitePermissionsRobot.Transition()
        }

        fun goBackToWebSiteExceptions(interact: SettingsSubMenuSitePermissionsExceptionsRobot.() -> Unit): SettingsSubMenuSitePermissionsExceptionsRobot.Transition {
            goBackButton().click()

            SettingsSubMenuSitePermissionsExceptionsRobot().interact()
            return SettingsSubMenuSitePermissionsExceptionsRobot.Transition()
        }
    }
}

private fun goBackButton() =
    onView(allOf(withContentDescription("Navigate up")))

private fun assertNavigationToolBarHeader() {
    onView(withText(R.string.preference_exceptions))
        .check((matches(withEffectiveVisibility(Visibility.VISIBLE))))
}

private fun assertExceptionDefault() =
    onView(allOf(withText(R.string.no_site_exceptions)))

private fun assertWebSiteInExceptionList() {
    mDevice.findObject(UiSelector().resourceId("$packageName:id/exceptions")).waitForExists(waitingTime)
    assertTrue(mDevice.findObject(UiSelector().textContains("localhost:")).waitForExists(waitingTime))
}

private fun assertCameraPermissionStatus(state: String) {
    mDevice.waitForIdle()
    mDevice.findObject(UiSelector().textContains(state)).waitForExists(waitingTime)

    onView(
        allOf(
            withText(state),
            hasSibling(withText("Camera"))
        )
    ).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun allowedButton() = onView(withId(R.id.ask_to_allow_radio))
private fun blockedButton() = onView(withId(R.id.block_radio))
private fun clearPermissionButton() = onView(withId(R.id.reset_permission))

private fun assertCameraPermissionView() {
    allowedButton().check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    blockedButton().check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    clearPermissionButton().check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun assertConfirmationDialog() =
    assertTrue(
        mDevice.findObject(
            UiSelector()
                .textContains("Are you sure that you want to clear this permission for this site?")
        ).waitForExists(waitingTime)
    )
