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
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.hamcrest.CoreMatchers
import org.mozilla.fenix.helpers.click
import org.mozilla.fenix.R

/**
 * Implementation of Robot Pattern for the settings Site Permissions Exceptions sub menu.
 */
class SettingsSubMenuSitePermissionsExceptionsRobot {

    fun verifyDefault() = assertExceptionDefault()!!

    fun verifyListedExceptionURL(url: String) = assertExceptionURL(url)

    fun verifyEmptyExceptionList() = assertEmptyExceptionList()

    fun clearAllSitePermissions() = deleteAllSitePermissions()

    fun clearAllPermissionsOnSite(website: String) = deleteAllPermissionsOnSite(website)

    fun clearSinglePermissionsOnSite(website: String, permission: String) =
        deleteSinglePermissionsOnSite(website, permission)

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

private fun assertEmptyExceptionList() {
    onView(withId(R.id.empty_exception_container))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun goBackButton() =
    onView(CoreMatchers.allOf(withContentDescription("Navigate up")))

private fun assertExceptionDefault() =
    onView(CoreMatchers.allOf(withText("Exceptions let you disable tracking protection for selected sites.")))

private fun assertExceptionURL(url: String) {
    onView(CoreMatchers.allOf(withText(url)))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
}

private fun deleteAllSitePermissions() {
    onView(withId(R.id.delete_all_site_permissions_button))
        .click()

    onView(withText("Clear permissions"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    onView(withText("Are you sure that you want to clear all the permissions on all sites?"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    onView(withText("CANCEL"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        .click()

    onView(withId(R.id.delete_all_site_permissions_button))
        .click()

    onView(withText("OK"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        .click()
}

private fun deleteAllPermissionsOnSite(website: String) {
    onView(CoreMatchers.allOf(withText(website)))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        .click()

    // Verify each permission has an icon
    // Verify each permission shows allowed/blocked state

    onView(withText("Clear permissions"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        .click()

    onView(withText("Clear permissions"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    onView(withText("Are you sure that you want to clear all the permissions for this site?"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    onView(withText("CANCEL"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        .click()

    onView(withText("Clear permissions"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        .click()

    onView(withText("OK"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        .click()
}

private fun deleteSinglePermissionsOnSite(website: String, permission: String) {
    onView(CoreMatchers.allOf(withText(website)))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        .click()

    onView(withText(permission))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        .click()

    onView(withText("Clear permission"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        .click()

    onView(withText("Are you sure that you want to clear this permission for this site?"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    onView(withText("CANCEL"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        .click()

    onView(withText("Clear permission"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        .click()

    onView(withText("OK"))
        .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        .click()

    Espresso.pressBack()
}
