/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.robots

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.containsString
import org.mozilla.fenix.helpers.TestAssetHelper
import org.mozilla.fenix.helpers.ext.waitNotNull

/**
 * Implementation of Robot Pattern for the Privacy Settings > saved logins sub menu
 */

class SettingsSubMenuLoginsAndPasswordsSavedLoginsRobot {
    fun verifySavedLoginsView() = assertSavedLoginsView()

    fun verifySavedLoginsAfterSync() {
        mDevice.waitNotNull(
            Until.findObjects(By.text("https://accounts.google.com")),
            TestAssetHelper.waitingTime
        )
        assertSavedLoginAppears()
    }

    fun tapSetupLater() = onView(ViewMatchers.withText("Later")).perform(ViewActions.click())

    fun verifySavedLoginFromPrompt() =
        mDevice.waitNotNull(Until.findObjects(By.text("test@example.com")))

    fun verifyNotSavedLoginFromPrompt() = onView(ViewMatchers.withText("test@example.com"))
        .check(ViewAssertions.doesNotExist())

    fun verifyLocalhostExceptionAdded() = onView(ViewMatchers.withText(containsString("localhost")))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

    class Transition {
        fun goBack(interact: SettingsSubMenuLoginsAndPasswordRobot.() -> Unit): SettingsSubMenuLoginsAndPasswordRobot.Transition {
            goBackButton().perform(ViewActions.click())

            SettingsSubMenuLoginsAndPasswordRobot().interact()
            return SettingsSubMenuLoginsAndPasswordRobot.Transition()
        }
    }
}

private fun goBackButton() =
    onView(CoreMatchers.allOf(ViewMatchers.withContentDescription("Navigate up")))

private fun assertSavedLoginsView() =
    onView(ViewMatchers.withText("Secure your logins and passwords"))
        .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

private fun assertSavedLoginAppears() = onView(ViewMatchers.withText("https://accounts.google.com"))
