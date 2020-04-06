/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("TooManyFunctions")

package org.mozilla.fenix.ui.robots

/**
 * Implementation of Robot Pattern for the settings Site Permissions Autoplay sub menu.
 */

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.hamcrest.CoreMatchers.allOf
import org.mozilla.fenix.R
import org.mozilla.fenix.helpers.assertIsChecked
import org.mozilla.fenix.helpers.click

class SettingsSubMenuSitePermissionsAutoPlayRobot {

    fun verifyNavigationToolBarHeader() = assertNavigationToolBarHeader()

    fun verifySitePermissionsAutoPlaySubMenuItems() {
        assertBlockAudioAndVideoOnMobileDataOnlyAudioAndVideoWillPlayOnWiFi()
        assertBlockAudioOnly()
        assertVideoAndAudioBlockedRecommended()
        assertCheckRadioButtonDefault()
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

private fun assertNavigationToolBarHeader() = onView(allOf(withContentDescription("AutoPlay")))

private fun assertBlockAudioAndVideoOnMobileDataOnlyAudioAndVideoWillPlayOnWiFi() =
    onView(withId(R.id.block_radio))
        .check((matches(withEffectiveVisibility(Visibility.VISIBLE))))

private fun assertBlockAudioOnly() = onView(withId(R.id.third_radio))
    .check((matches(withEffectiveVisibility(Visibility.VISIBLE))))

private fun assertVideoAndAudioBlockedRecommended() = onView(withId(R.id.fourth_radio))
    .check((matches(withEffectiveVisibility(Visibility.VISIBLE))))

private fun assertCheckRadioButtonDefault() {

    onView(withId(R.id.block_radio))
        .assertIsChecked(isChecked = false)

    onView(withId(R.id.third_radio))
        .assertIsChecked(isChecked = false)

    onView(withId(R.id.fourth_radio))
        .assertIsChecked(isChecked = true)
}

private fun goBackButton() =
    onView(allOf(withContentDescription("Navigate up")))
